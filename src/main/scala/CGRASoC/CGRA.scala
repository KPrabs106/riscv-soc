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

case class CGRAParams(address: BigInt, beatBytes: Int)

class CGRABase(w: Int) extends Module {
  // TODO fix IO
  val io = IO(new Bundle {
    val data_out = Output(UInt(w.W))
    val done = Output(Bool())
    val enable = Input(Bool())
    val address_in = Input(UInt(w.W))
    val data_in = Input(UInt(w.W))
  })

  // TODO add CGRA functionality
  io.done := io.enable
  io.data_out := io.data_in + 1.U
}

trait CGRABundle extends Bundle {
  val data_out = Output(UInt(64.W))
  val done = Output(Bool())
}

trait CGRAModule extends HasRegMap {
  val io: CGRABundle
  implicit val p: Parameters
  def params: CGRAParams
  
  // TODO define registers
  val enable = RegInit(false.B)
  val address_in = Reg(UInt(64.W))
  val data_in = Reg(UInt(64.W))
  val data_out = Reg(UInt(64.W))
  val done = RegInit(false.B)

  val base = Module(new CGRABase(64))
  io.data_out := base.io.data_out
  io.done := base.io.done
  base.io.enable := enable
  base.io.address_in := address_in
  base.io.data_in := data_in

  // TODO define regmap
  regmap(
    0x00 -> Seq(
      RegField(1,enable)),
    0x08 -> Seq(
      RegField(64, address_in)),
    0x10 -> Seq(
      RegField(64, data_in)),
    0x18 -> Seq(
      RegField(64, data_out)),
    0x20 -> Seq(
      RegField(1, done))
  )
}

class CGRATL(c: CGRAParams)(implicit p: Parameters)
  extends TLRegisterRouter(
    c.address, "cgra", Seq("cgra"),
    beatBytes = c.beatBytes)(
      new TLRegBundle(c, _) with CGRABundle)(
      new TLRegModule(c, _, _) with CGRAModule)

trait HasPeripheryCGRATL { this: BaseSubsystem =>
  implicit val p: Parameters
  
  private val address = 0x4000
  private val portName = "cgra"

  val cgra = LazyModule(new CGRATL(
    CGRAParams(address, pbus.beatBytes))(p))

  pbus.toVariableWidthSlave(Some(portName)) { cgra.node }
}

trait HasPeripheryCGRATLModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryCGRATL

  val done = IO(Output(Bool()))
  val data_out = IO(Output(UInt(64.W)))

  done := outer.cgra.module.io.done 
  data_out := outer.cgra.module.io.data_out
  
}
