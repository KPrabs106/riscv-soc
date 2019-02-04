package CGRASoC

import chisel3._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.DontTouch
import testchipip._

class Top(implicit p: Parameters) extends RocketSubsystem
    with CanHaveMasterAXI4MemPort
    with HasPeripheryBootROM
//  with HasSystemErrorSlave
    with HasSyncExtInterrupts
    with HasNoDebug
    with HasPeripherySerial {
  override lazy val module = new TopModule(this)
}

class TopModule[+L <: Top](l: L) extends RocketSubsystemModuleImp(l)
    with HasRTCModuleImp
    with CanHaveMasterAXI4MemPortModuleImp
    with HasPeripheryBootROMModuleImp
    with HasExtInterruptsModuleImp
    with HasNoDebugModuleImp
    with HasPeripherySerialModuleImp
    with DontTouch

class TopWithPWMTL(implicit p: Parameters) extends Top
    with HasPeripheryPWMTL {
  override lazy val module = new TopWithPWMTLModule(this)
}

class TopWithPWMTLModule(l: TopWithPWMTL)
  extends TopModule(l) with HasPeripheryPWMTLModuleImp

class TopWithPHY(implicit p: Parameters) extends Top
    with HasPeripheryPHY {
  override lazy val module = new TopWithPHYModule(this)
}

class TopWithPHYModule(l: TopWithPHY)
  extends TopModule(l) with HasPeripheryPHYModuleImp

class TopWithPWMAXI4(implicit p: Parameters) extends Top
    with HasPeripheryPWMAXI4 {
  override lazy val module = new TopWithPWMAXI4Module(this)
}

class TopWithPWMAXI4Module(l: TopWithPWMAXI4)
  extends TopModule(l) with HasPeripheryPWMAXI4ModuleImp

class TopWithBlockDevice(implicit p: Parameters) extends Top
    with HasPeripheryBlockDevice {
  override lazy val module = new TopWithBlockDeviceModule(this)
}

class TopWithBlockDeviceModule(l: TopWithBlockDevice)
  extends TopModule(l)
  with HasPeripheryBlockDeviceModuleImp
