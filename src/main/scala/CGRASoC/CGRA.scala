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
    val cgra_out = Output(Bool())
    val enable = Input(Bool())
  })

  // TODO add CGRA functionality
  io.cgra_out := io.enable
}

trait CGRABundle extends Bundle {
  val cgra_out = Output(Bool())
}

trait CGRAModule extends HasRegMap {
  val io: CGRABundle
  implicit val p: Parameters
  def params: CGRAParams
  
  // TODO define registers
  val enable = RegInit(false.B)

  val base = Module(new CGRABase(32))
  io.cgra_out := base.io.cgra_out
  base.io.enable := enable

  // TODO define regmap
  regmap(
    0x00 -> Seq(
      RegField(1,enable)
    )
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

  val cgra_out = IO(Output(Bool()))

  cgra_out := outer.cgra.module.io.cgra_out
}
