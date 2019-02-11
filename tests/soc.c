#define PHY_ENABLE 0x2008
#define PHY_DONE 0x2028
#define PHY_DATA_OUT 0x2018
//#define PHY_COUNT 0x2018
//#define PHY_DATA_IN 0x2020
#define PHY_DATA_IN 0x2020

#define CGRA_ENABLE 0x4000
#define CGRA_LOAD_ADDRESS 0x4008
#define CGRA_STORE_ADDRESS 0x4010
#define CGRA_LOAD_DATA 0x4018
#define CGRA_STORE_DATA 0x4020
#define CGRA_DONE 0x4028

#include <stdio.h>
#include <time.h>
#include "minunit.h"
#include "mmio.h"

uint64_t write_data[1];

MU_TEST(phy_unit_test){

	reg_write32(PHY_ENABLE, 1);
	uint32_t enable = reg_read32(PHY_ENABLE);
	mu_check(enable == 1);


	uint32_t done = 0;
	while (done == 0){
		done = reg_read32(PHY_DONE);
	}

	uint32_t ld_addr = reg_read32(CGRA_LOAD_ADDRESS);
	uint32_t st_addr = reg_read32(CGRA_STORE_ADDRESS);
	uint32_t cgra_en = reg_read32(CGRA_ENABLE);

	mu_check(ld_addr == 0x2014);
	mu_check(st_addr == 0x2018);
	mu_check(cgra_en == 0x0001); //check whatever you decide to set cgra_en to 
								 //(when I tried 0x0001 it wouldnt run the sim??)

	
	/*reg_write32(CGRA_ENABLE, 1);


	//uint32_t done;
	uint16_t timer = 1;
	while( (done = reg_read32(CGRA_DONE)) == 0){
		if(++timer == 0){
			mu_fail("timed out!");
			break;
		}
	}*/
	
	uint32_t phy_data = reg_read32(PHY_DATA_OUT);
	//uint32_t cgra_data = reg_read32(PHY_DATA_IN);


	mu_check(phy_data == 255);
	//mu_check(cgra_data == 256);

}

MU_TEST_SUITE(PHY_test_suite){
	MU_RUN_TEST(phy_unit_test);

	MU_SUITE_REPORT();
}

MU_TEST(cgra_unit_test){
	#define INITIAL_DATA 0x01234567
	write_data[0] = INITIAL_DATA;
	reg_write64(CGRA_LOAD_ADDRESS, write_data);
	reg_write64(CGRA_STORE_ADDRESS, write_data);

	asm volatile ("fence");

	uint64_t load_address = reg_read64(CGRA_LOAD_ADDRESS);
	mu_check(load_address == write_data);

	uint64_t store_address = reg_read64(CGRA_STORE_ADDRESS);
	mu_check(store_address == write_data);

	reg_write32(CGRA_ENABLE, 1);

	asm volatile ("fence");

volatile	uint32_t enable = reg_read32(CGRA_ENABLE);
	mu_check(enable == 1);

	uint32_t done;
	uint16_t timer = 1;
	while( (done = reg_read32(CGRA_DONE)) == 0){
		if(++timer == 0){
			mu_fail("timed out!");
			break;
		}
	}
	
	uint64_t data_in = reg_read64(CGRA_LOAD_DATA);
	mu_check(data_in == INITIAL_DATA);

	uint64_t data_out = reg_read64(CGRA_STORE_DATA);
	mu_check(data_out == INITIAL_DATA + 1);
	
	printf("%016lx\n", write_data[0]);
	mu_check(write_data[0] == INITIAL_DATA + 1);
}

MU_TEST_SUITE(CGRA_test_suite){
	MU_RUN_TEST(cgra_unit_test);
	
	MU_SUITE_REPORT();
}

MU_TEST(integration_test){
	reg_write32(PHY_ENABLE, 1);
	uint32_t enable = reg_read32(PHY_ENABLE);
	mu_check(enable == 1);

	reg_write32(CGRA_LOAD_ADDRESS, PHY_DATA_OUT);
	//reg_write32(CGRA_ENABLE, 1);
	uint32_t ld_addr = reg_read32(CGRA_LOAD_ADDRESS);
	uint32_t st_addr = reg_read32(CGRA_STORE_ADDRESS);
	//uint32_t cgra_en = reg_read32(CGRA_ENABLE);

	//printf("cgra_en = %x\n", cgra_en);
	//printf("st_addr = %x\n", st_addr);
	//printf("ld_addr = %x\n", ld_addr);

	mu_check(ld_addr == 0x2018);
	mu_check(st_addr == 0x2020);
	//mu_check(cgra_en == 1);

	uint32_t phy_data = reg_read32(PHY_DATA_IN);
	//printf("phy_data = %x", phy_data);
	mu_check(phy_data == 256);

}

MU_TEST_SUITE(integration_test_suite){
	MU_RUN_TEST(integration_test);
	
	MU_SUITE_REPORT();
}

int main(void)
{
	//MU_RUN_SUITE(PHY_test_suite);

	//MU_RUN_SUITE(CGRA_test_suite);

	MU_RUN_SUITE(integration_test_suite);

	MU_REPORT();

	return 0;
}
