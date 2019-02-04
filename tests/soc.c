#include <stdio.h>

#include "minunit.h"
#include "mmio.h"


MU_TEST(test_check_pass){
	reg_write32(PHY_ENABLE, 1);
	
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

MU_TEST(test_check_fail){
	mu_check(0 == 1);
}

MU_TEST_SUITE(PHY_test_suite){
	MU_RUN_TEST(test_check_pass);
	//MU_RUN_TEST(test_check_fail);

	MU_SUITE_REPORT();
}

MU_TEST_SUITE(CGRA_test_suite){
	MU_RUN_TEST(test_check_pass);
	MU_RUN_TEST(test_check_fail);
	
	MU_SUITE_REPORT();
}

int main(void)
{
	MU_RUN_SUITE(PHY_test_suite);

	MU_RUN_SUITE(CGRA_test_suite);

	MU_REPORT();

	return 0;
}
