package CGRASoC

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

class CGRA(address: BigInt, beatBytes: Int)(implicit p: Parameters) extends LazyModule {
  val device = new SimpleDevice("cgra", Seq("ee272,cgra"))

  val regnode = TLRegisterNode(
    address = Seq(AddressSet(address, 0x3f)),
    device = device,
    beatBytes = beatBytes)

  val dmanode = TLClientNode(Seq(TLClientPortParameters(
    Seq(TLClientParameters(
      name = "cgra", sourceId = IdRange(0, 1))))))

  lazy val module = new CGRAModuleImp(this)
}

class CGRAModuleImp(outer: CGRA) extends LazyModuleImp(outer) {
  val (tl, edge) = outer.dmanode.out(0)
  val w = 64
  val beatBytes = (edge.bundle.dataBits / 8)

  val enable = RegInit(false.B)
  val load_address = Reg(UInt(w.W))
  val store_address = Reg(UInt(w.W))
  val data_in = Reg(UInt(w.W))
  val data_out = Reg(UInt(w.W))
  val done = RegInit(false.B)

  // state machine for CGRA
  val s_idle :: s_fetch_1 :: s_fetch_2 :: s_exec_1 :: s_exec_2 :: s_wb_1 :: s_wb_2 :: Nil = Enum(7)
  val state = RegInit(s_idle)

  val get_req = edge.Get(
    fromSource = 0.U,
    toAddress = load_address,
    lgSize = log2Ceil(beatBytes).U)._2
  val put_req = edge.Put(
    fromSource = 0.U,
    toAddress = (store_address >> log2Ceil(beatBytes)-1) << log2Ceil(beatBytes)-1,
    lgSize = log2Ceil(beatBytes).U,
    data = data_out)._2

  tl.a.valid := (state === s_fetch_1) || (state === s_wb_1)
  tl.a.bits := Mux(state === s_fetch_1, get_req, put_req)

  tl.d.ready := (state === s_fetch_2) || (state === s_wb_2)

  when(state === s_idle && enable){ // CGRA has been enabled, go to fetch_1
    state := s_fetch_1
  }

  when(state === s_fetch_1 && tl.a.fire()){ // get request has been sent
    state := s_fetch_2
    printf("sent get request to addr: %x\n", load_address)
  }

  when(state === s_fetch_2 && tl.d.fire()){ // receieved response to get request
    printf("received data: %x\n", tl.d.bits.data)
    data_in := tl.d.bits.data
    state := s_exec_1
  }

  when(state === s_exec_1){
    data_out := data_in + 1.U // perform trivial operation
    state := s_exec_2
    assert(store_address(log2Ceil(beatBytes)-1,0) === 0.U,
    s"Store address not aligned to ${beatBytes} bytes")
  }

  when(state === s_exec_2){
    state := s_wb_1
  }

  when(state === s_wb_1 && tl.a.fire()){ // put request has been sent
    state := s_wb_2
    printf("sent put request to addr: %x with data: %x\n", (store_address >> log2Ceil(beatBytes)-1) << log2Ceil(beatBytes)-1, data_out)
  }

  when(state === s_wb_2 && tl.d.fire()){ // received response to put request
    printf("received response to put\n")
    state := s_idle
    done := true.B
    enable := false.B
  }

  outer.regnode.regmap(
    0x00 -> Seq(
      RegField(1,enable)),
    0x08 -> Seq(
      RegField(w, load_address)),
    0x10 -> Seq(
      RegField(w, store_address)),
    0x18 -> Seq(
      RegField(w, data_in)),
    0x20 -> Seq(
      RegField(w, data_out)),
    0x28 -> Seq(
      RegField(1, done)),
  )
}

trait HasPeripheryCGRATL { this: BaseSubsystem =>
  implicit val p: Parameters
  
  private val address = 0x4000
  private val portName = "cgra"

  val cgra = LazyModule(new CGRA(address, pbus.beatBytes)(p))

  pbus.toVariableWidthSlave(Some(portName)) { cgra.regnode }
  sbus.fromPort(Some(portName))() := cgra.dmanode
}

trait HasPeripheryCGRATLModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryCGRATL
}
