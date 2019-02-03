#include <stdio.h>

#include "minunit.h"
#include "mmio.h"


MU_TEST(test_check_pass){
	mu_check(1 == 1);
}

MU_TEST(test_check_fail){
	mu_check(0 == 1);
}

MU_TEST_SUITE(PHY_test_suite){
	MU_RUN_TEST(test_check_pass);
	MU_RUN_TEST(test_check_fail);

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
