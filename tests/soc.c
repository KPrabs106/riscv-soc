#define PHY_ENABLE 0x2008
#define PHY_DONE 0x2010
#define PHY_DATA_OUT 0x2014
#define PHY_COUNT 0x2018
#define PHY_DATA_IN 0x2020

#define CGRA_ENABLE 0x4000
#define CGRA_DONE 0x4008
#define CGRA_DATA_IN 0x4010
#define CGRA_ADDRESS_IN 0x4018
#define CGRA_DATA_OUT 0x4020

#include <stdio.h>
#include "minunit.h"
#include "mmio.h"


MU_TEST(phy_unit_test){
	reg_write32(PHY_ENABLE, 1);

	uint32_t enable = reg_read32(PHY_ENABLE);
	mu_check(enable == 1);

	uint32_t done = 0;
	while (done == 0){
		done = reg_read32(PHY_DONE);
	}
	
	uint32_t phy_data = reg_read32(PHY_DATA_OUT);
	uint32_t count = reg_read32(PHY_COUNT);

	reg_write32(PHY_ENABLE, 0);

	mu_check(phy_data == -1);
	mu_check(count == 6);
}

MU_TEST_SUITE(PHY_test_suite){
	MU_RUN_TEST(phy_unit_test);

	MU_SUITE_REPORT();
}

MU_TEST(cgra_unit_test){
	reg_write32(CGRA_ENABLE, 1);
	uint32_t enable = reg_read32(CGRA_ENABLE);
	mu_check(enable == 1);

	uint64_t data = 0x01234567;
	reg_write64(CGRA_ADDRESS_IN, &data);
	uint64_t address = reg_read32(CGRA_ADDRESS_IN);
	mu_check(address == &data);
	
	uint32_t done;
	while( (done = reg_read32(CGRA_DONE)) == 0);
	
	uint64_t data_in = reg_read64(CGRA_DATA_IN);
	mu_check(data_in == data);

	uint64_t data_out = reg_read64(CGRA_DATA_OUT);
	mu_check(data_out == data + 1);
}

MU_TEST_SUITE(CGRA_test_suite){
	MU_RUN_TEST(cgra_unit_test);
	
	MU_SUITE_REPORT();
}

MU_TEST(integration_test){
	reg_write32(PHY_ENABLE, 1);
	uint32_t phy_data_out = reg_read32(PHY_DATA_OUT);

	reg_write32(CGRA_ADDRESS_IN, PHY_DATA_OUT);
	reg_write32(CGRA_ENABLE, 1);

	uint32_t done;
	while( (done = reg_read32(CGRA_DONE)) == 0);

	uint32_t cgra_data_in = reg_read32(CGRA_DATA_IN);
	mu_check(cgra_data_in == phy_data_out);

	uint32_t cgra_data_out = reg_read32(CGRA_DATA_OUT);
	mu_check(cgra_data_out == phy_data_out + 1);

	uint32_t phy_data_in = reg_read32(PHY_DATA_IN);
	mu_check(phy_data_in == cgra_data_out);

}

MU_TEST_SUITE(integration_test_suite){
	MU_RUN_TEST(integration_test);
	
	MU_SUITE_REPORT();
}

int main(void)
{
	MU_RUN_SUITE(PHY_test_suite);

	MU_RUN_SUITE(CGRA_test_suite);

	MU_RUN_SUITE(integration_test_suite);

	MU_REPORT();

	return 0;
}
