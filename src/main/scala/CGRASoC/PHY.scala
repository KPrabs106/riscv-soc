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

/*case class PHYParams(address: BigInt, beatBytes: Int)

class PHYBase(w: Int) extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val data_out = Output(UInt(w.W))
    val enable = Input(Bool())
    val intest = Input(UInt(w.W))
    val count = Output(UInt(w.W))
  })

  // The counter should count up until period is reached
  val s_idle :: s_send :: s_done :: Nil = Enum(Bits(), 3)
  val state = Reg(init = s_idle)

  val d_out_r = Reg(UInt(w.W))
  val done_r = Reg(Bool())

  val count_r = Reg(UInt(w.W))

  when (state === s_idle){
    when (io.enable) {state := s_send}
    d_out_r := 0.U
    done_r := false.B
    count_r := 0.U
  }
  when (state === s_send){
    when(count_r >= 5.U) {state := s_done}
    d_out_r := "h_ffff_ffff".U
    done_r := false.B
    count_r := count_r + 1.U
  }
  when (state === s_done){
    when (!io.enable) {state := s_idle}
    d_out_r := "h_ffff_ffff".U
    done_r := true.B
    count_r := count_r
  }

  io.data_out := d_out_r
  io.done := done_r
  io.count := count_r

}

trait PHYBundle extends Bundle {
  val done = Output(Bool())
  val data_out = Output(UInt(32.W))
  val count = Output(UInt(32.W))
}

trait PHYModule extends HasRegMap {
  val io: PHYBundle
  implicit val p: Parameters
  def params: PHYParams

  val enable = RegInit(false.B)
  val intest = RegInit(0.U)

  val base = Module(new PHYBase(32))
  io.done := base.io.done
  io.data_out := base.io.data_out
  io.count := base.io.count
  base.io.enable := enable
  base.io.intest := intest

  regmap(
    0x08 -> Seq(
      RegField(1, enable)),
    0x10 -> Seq(
      RegField(1, io.done)),
    0x14 -> Seq(
      RegField(32, io.data_out)),
    0x18 -> Seq(
      RegField(32, io.count)))
}

class PHYTL(c: PHYParams)(implicit p: Parameters)
  extends TLRegisterRouter(
    c.address, "phy", Seq("phy"),
    beatBytes = c.beatBytes)(
      new TLRegBundle(c, _) with PHYBundle)(
      new TLRegModule(c, _, _) with PHYModule)

trait HasPeripheryPHYTL { this: BaseSubsystem =>
  implicit val p: Parameters

  private val address = 0x2000
  private val portName = "phy"

  val phy = LazyModule(new PHYTL(
    PHYParams(address, pbus.beatBytes))(p))

  pbus.toVariableWidthSlave(Some(portName)) { phy.node }
}

trait HasPeripheryPHYTLModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryPHYTL

  val phy_done = IO(Output(Bool()))
  val phy_data_out = IO(Output(UInt(32.W)))
  val phy_count = IO(Output(UInt(32.W)))
  phy_data_out := outer.phy.module.io.data_out
  phy_done := outer.phy.module.io.done
  phy_count := outer.phy.module.io.count
}*/

class PHY(
    address: BigInt,
    val beatBytes: Int = 8)
    (implicit p: Parameters) extends LazyModule {

  val device = new SimpleDevice("phy", Seq("example,phy"))
  val regnode = TLRegisterNode(
    address = Seq(AddressSet(address, 0x3f)),
    device = device,
    beatBytes = beatBytes)
  val dmanode = TLClientNode(Seq(TLClientPortParameters(
    Seq(TLClientParameters(
      name = "phy",
      sourceId = IdRange(0, 1))))))
  lazy val module = new PHYModuleImp(this)
}

class PHYModuleImp(outer: PHY) extends LazyModuleImp(outer) {
  val (tl, edge) = outer.dmanode.out(0)
  val w = 64
  val beatBytes = (edge.bundle.dataBits / 8)

  val done = Reg(Bool())
    val data_out = Reg(UInt(w.W))
    val data_in = Reg(UInt(w.W))
    val enable = Reg(Bool())
    val cgra_enable = Reg(Bool())
    val intest = Reg(UInt(w.W))
    val count = Reg(UInt(w.W))
    val load_address = Reg(UInt(w.W))


    val s_idle :: s_ld_addr :: s_ld_addr2 :: s_st_addr :: s_st_addr2 :: s_cgra_en :: s_cgra_en2 :: s_send:: s_receive :: Nil = Enum(9)
  val state = RegInit(s_idle)


  val get_req = edge.Get(
      fromSource = 0.U,
      toAddress = load_address,
      lgSize = log2Ceil(beatBytes).U)._2
    val put_req_ld = edge.Put(
      fromSource = 0.U,
      toAddress = 0x4008.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x2014.U)._2
    val put_req_st = edge.Put(
      fromSource = 0.U,
      toAddress = 0x4010.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x2018.U)._2
    val put_req_en = edge.Put(
      fromSource = 0.U,
      toAddress = 0x4000.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x2014.U)._2


 //tl.a.valid := state === s_idle
  //tl.a.bits := Mux(state === s_idle, put_req, get_req)

    tl.a.valid := (state === s_ld_addr) || (state === s_st_addr) || (state === s_cgra_en)
    cgra_enable := (state === s_cgra_en) || (state === s_send)
    tl.d.ready := (state === s_ld_addr) || (state === s_st_addr) || (state === s_cgra_en)
    

    done := (state === s_receive)

    when (state === s_idle){
      when(enable) {state := s_ld_addr}
      data_out := 0.U
    }

    when (state === s_ld_addr){
      tl.a.bits := put_req_ld
      when(tl.a.fire()){state := s_ld_addr2} //ld addr put req sent
      //state := s_st_addr
      data_out := 0.U
      //state := s_send
    }

    when (state === s_ld_addr2){
      when(tl.d.fire()){  //ld addr put req response received
        state := s_st_addr
      }
      data_out := 0.U
    }

    when (state === s_st_addr){
      tl.a.bits := put_req_st
      when(tl.a.fire()){state := s_st_addr2} //st addr put req sent
      data_out := 0.U
    }

    when (state === s_st_addr2){
      when(tl.d.fire()){ //st addre put req response received
        state := s_cgra_en
      }
      data_out := 0.U
    }

    when (state === s_cgra_en){
      tl.a.bits := put_req_en
      when(tl.a.fire()){state := s_cgra_en2} //cgra enable put req sent
      data_out := 0.U
    }

    when (state === s_cgra_en2){
      when(tl.d.fire()){ //cgra enable put req response received
        state := s_send
      }
      data_out := 0.U
    }

    when (state === s_send){
      data_out := 255.U
      state := s_receive
    }
    when (state === s_receive){
      data_out := 255.U
      when ((data_in === 256.U) && !enable)
      {
        state := s_idle
      }
    }
    


  outer.regnode.regmap(
    0x08 -> Seq(
      RegField(1, enable)),
    0x14 -> Seq(
      RegField(32, data_out)),
    0x18 -> Seq(
      RegField(32, data_in)),
    0x24 -> Seq(
      RegField(1, cgra_enable)),
    0x28 -> Seq(
      RegField(1, done)))


}


trait HasPeripheryPHYTL { this: BaseSubsystem =>
  implicit val p: Parameters

  private val address = 0x2000
  private val portName = "phy"

  val streamWidth = pbus.beatBytes * 8
    val phy = LazyModule(new PHY(address, pbus.beatBytes)(p))

  pbus.toVariableWidthSlave(Some(portName)) { phy.regnode }
  sbus.fromPort(Some(portName))() := phy.dmanode
}


trait HasPeripheryPHYTLModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryPHYTL

}


