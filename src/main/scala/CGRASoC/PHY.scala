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
    //val cgra_enable = Reg(Bool())  
    //val intest = Reg(UInt(w.W))
    val count = Reg(UInt(w.W))
    //val load_address = Reg(UInt(w.W))


    val s_idle :: s_ld_addr :: s_ld_addr2 :: s_st_addr :: s_st_addr2 :: s_cgra_en :: s_cgra_en2 :: s_send_receive :: Nil = Enum(8)
  val state = RegInit(s_idle)


  /*val get_req = edge.Get(
      fromSource = 0.U,
      toAddress = load_address,
      lgSize = log2Ceil(beatBytes).U)._2*/
    val put_req_ld = edge.Put(  //send 0x2014 to load_address
      fromSource = 0.U,
      toAddress = 0x4008.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x2014.U)._2
    val put_req_st = edge.Put(  //send 0x2018 to store_address
      fromSource = 0.U,
      toAddress = 0x4010.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x2018.U)._2
    val put_req_en = edge.Put(  //send some value to CGRA's enable (needs to be 32 bits)
      fromSource = 0.U,
      toAddress = 0x4030.U,
      lgSize = log2Ceil(beatBytes).U,
      data = 0x0080.U)._2  


    tl.a.valid := (state === s_ld_addr) || (state === s_st_addr) || (state === s_cgra_en)
    //cgra_enable := (state === s_send)
    tl.d.ready := (state === s_ld_addr2) || (state === s_st_addr2) || (state === s_cgra_en2)
    
    done := (state === s_send_receive)


    when (state === s_idle){
      when(enable) {state := s_ld_addr}
      data_out := 0.U
    }

    when (state === s_ld_addr){
      tl.a.bits := put_req_ld
      when(tl.a.fire()){state := s_ld_addr2} //ld addr put req sent
      data_out := 0.U
      
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
        state := s_send_receive
      }
      data_out := 0.U
    }

    /*when (state === s_send){ 
      data_out := 255.U
      state := s_receive
    }*/

    //decided to combine send and receive state to help prevent timing issues
    when (state === s_send_receive){  
      data_out := 255.U  
      when ((data_in === 256.U) && !enable) //wait for cgra to send back data and cpu to disable phy
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
   // 0x04 -> Seq(
     // RegField(1, cgra_enable)),
    0x28 -> Seq(
      RegField(1, done)))
}


trait HasPeripheryPHYTL { this: BaseSubsystem =>
  implicit val p: Parameters

  private val address = 0x2000
  private val portName = "phy"

  //val streamWidth = pbus.beatBytes * 8
    val phy = LazyModule(new PHY(address, pbus.beatBytes)(p))

  pbus.toVariableWidthSlave(Some(portName)) { phy.regnode }
  sbus.fromPort(Some(portName))() := phy.dmanode
}


trait HasPeripheryPHYTLModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryPHYTL

}


