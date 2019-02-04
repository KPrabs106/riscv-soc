package example

import chisel3._
import chisel3.util._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

case class PHYParams(address: BigInt, beatBytes: Int)

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

trait PHYTLBundle extends Bundle {
  val done = Output(Bool())
  val data_out = Output(UInt(32.W))
  val count = Output(UInt(32.W))
}

trait PHYTLModule extends HasRegMap {
  val io: PHYTLBundle
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
    c.address, "phy", Seq("ucbbar,phy"),
    beatBytes = c.beatBytes)(
      new TLRegBundle(c, _) with PHYTLBundle)(
      new TLRegModule(c, _, _) with PHYTLModule)

trait HasPeripheryPHY { this: BaseSubsystem =>
  implicit val p: Parameters

  private val address = 0x2000
  private val portName = "phy"

  val phy = LazyModule(new PHYTL(
    PHYParams(address, pbus.beatBytes))(p))

  pbus.toVariableWidthSlave(Some(portName)) { phy.node }
}

trait HasPeripheryPHYModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryPHY

  val done = IO(Output(Bool()))
  val data_out = IO(Output(UInt(32.W)))
  val count = IO(Output(UInt(32.W)))
  data_out := outer.phy.module.io.data_out
  done := outer.phy.module.io.done
  count := outer.phy.module.io.count
}
