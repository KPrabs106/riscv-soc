# CGRA SoC with a RISC-V CPU

## Getting started

### Checking out the sources

After cloning this repo, you will need to initialize all of the submodules

    git clone https://github.com/KPrabs106/riscv-soc.git
    cd project-template
    git submodule update --init --recursive

### Building the tools

The tools repo contains the cross-compiler toolchain, frontend server, and
proxy kernel, which you will need in order to compile code to RISC-V
instructions and run them on your design. There are detailed instructions at
https://github.com/riscv/riscv-tools. But to get a basic installation, just
the following steps are necessary.

    # You may want to add the following two lines to your shell profile
    export RISCV=/path/to/install/dir
    export PATH=$RISCV/bin:$PATH

    cd rocket-chip/riscv-tools
    ./build.sh

### Compiling the C tests

To compile the C tests:

    $ cd tests
    $ make

### Compiling and running the Verilator simulation

To compile the design:
    
    $ cd verisim
    $ make
    
An executable called simulator-DefaultConfig will be produced.
You can then use this executable to run any compatible RV64 code. For example, to run the SoC tests:

    ./simulator-DefaultConfig ../tests/soc.riscv

To get additional information, such as a disassembled instruction trace:

    ./simulator-DefaultConfig +verbose ../tests/soc.riscv 2>&1 | spike-dasm
    
To generate a VCD waveform:
    
    $ make debug
    $ /simulator-DefaultConfig-debug -voutput.vcd ../tests/soc.riscv
    

### Generating synthesizable Verilog

To generate synthesizable Verilog:

    $ cd vsim
    $ make verilog

The Verilog will be generated in vsim/generated-src.
