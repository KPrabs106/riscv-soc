/*
 * Copyright (c) 2012 David Siñuela Pastor, siu.4coders@gmail.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
#ifndef __MINUNIT_H__
#define __MINUNIT_H__

#include <unistd.h>	/* POSIX flags */
#include <sys/resource.h>
#include <sys/times.h>
#include <string.h>

#include <stdio.h>
#include <math.h>

/*  Maximum length of last message */
#define MINUNIT_MESSAGE_LEN 1024
/*  Accuracy with which floats are compared */
#define MINUNIT_EPSILON 1E-12

/*  Misc. counters */
static int minunit_run = 0;
static int minunit_assert = 0;
static int minunit_fail = 0;
static int minunit_status = 0;
static int suite_minunit_run = 0;
static int suite_minunit_assert = 0;
static int suite_minunit_fail = 0;

/*  Last message */
static char minunit_last_message[MINUNIT_MESSAGE_LEN];

/*  Test setup and teardown function pointers */
static void (*minunit_setup)(void) = NULL;
static void (*minunit_teardown)(void) = NULL;

/*  Definitions */
#define MU_TEST(method_name) static void method_name(void)
#define MU_TEST_SUITE(suite_name) static void suite_name(void)

#define MU__SAFE_BLOCK(block) do {\
	block\
} while(0)

/*  Run test suite and unset setup and teardown functions */
#define MU_RUN_SUITE(suite_name) MU__SAFE_BLOCK(\
	printf("\n\nRunning suite: %s\n", #suite_name);\
	suite_minunit_run = 0;\
	suite_minunit_assert = 0;\
	suite_minunit_fail = 0;\
	suite_name();\
	minunit_setup = NULL;\
	minunit_teardown = NULL;\
)

/*  Configure setup and teardown functions */
#define MU_SUITE_CONFIGURE(setup_fun, teardown_fun) MU__SAFE_BLOCK(\
	minunit_setup = setup_fun;\
	minunit_teardown = teardown_fun;\
)

/*  Test runner */
#define MU_RUN_TEST(test) MU__SAFE_BLOCK(\
	if (minunit_setup) (*minunit_setup)();\
	minunit_status = 0;\
	test();\
	minunit_run++;\
	suite_minunit_run++;\
	if (minunit_status) {\
		minunit_fail++;\
		suite_minunit_fail++;\
		printf("F");\
		printf("\n%s\n", minunit_last_message);\
	}\
	if (minunit_teardown) (*minunit_teardown)();\
)

/*  Report */
#define MU_SUITE_REPORT() MU__SAFE_BLOCK(\
	printf("\nSuite Results:\n%d tests, %d assertions, %d failures\n", suite_minunit_run, suite_minunit_assert, suite_minunit_fail);\
)
#define MU_REPORT() MU__SAFE_BLOCK(\
	printf("\n\nFinal Results:\n%d tests, %d assertions, %d failures\n\n", minunit_run, minunit_assert, minunit_fail);\
)

/*  Assertions */
#define mu_check(test) MU__SAFE_BLOCK(\
	minunit_assert++;\
	suite_minunit_assert++;\
	if (!(test)) {\
		sprintf(minunit_last_message, "%s failed:\n\t%s:%d: %s", __func__, __FILE__, __LINE__, #test);\
		minunit_status = 1;\
		return;\
	} else {\
		printf("P");\
	}\
)

#define mu_fail(message) MU__SAFE_BLOCK(\
	minunit_assert++;\
	suite_minunit_assert++;\
	sprintf(minunit_last_message, "%s failed:\n\t%s:%d: %s", __func__, __FILE__, __LINE__, message);\
	minunit_status = 1;\
	return;\
)

#define mu_assert(test, message) MU__SAFE_BLOCK(\
	minunit_assert++;\
	suite_minunit_assert++;\
	if (!(test)) {\
		sprintf(minunit_last_message, "%s failed:\n\t%s:%d: %s", __func__, __FILE__, __LINE__, message);\
		minunit_status = 1;\
		return;\
	} else {\
		printf("P");\
	}\
)

#define mu_assert_int_eq(expected, result) MU__SAFE_BLOCK(\
	int minunit_tmp_e;\
	int minunit_tmp_r;\
	minunit_assert++;\
	suite_minunit_assert++;\
	minunit_tmp_e = (expected);\
	minunit_tmp_r = (result);\
	if (minunit_tmp_e != minunit_tmp_r) {\
		sprintf(minunit_last_message, "%s failed:\n\t%s:%d: %d expected but was %d", __func__, __FILE__, __LINE__, minunit_tmp_e, minunit_tmp_r);\
		minunit_status = 1;\
		return;\
	} else {\
		printf("P");\
	}\
)

#define mu_assert_double_eq(expected, result) MU__SAFE_BLOCK(\
	double minunit_tmp_e;\
	double minunit_tmp_r;\
	minunit_assert++;\
	suite_minunit_assert++;\
	minunit_tmp_e = (expected);\
	minunit_tmp_r = (result);\
	if (fabs(minunit_tmp_e-minunit_tmp_r) > MINUNIT_EPSILON) {\
		int minunit_significant_figures = 1 - log10(MINUNIT_EPSILON);\
		sprintf(minunit_last_message, "%s failed:\n\t%s:%d: %.*g expected but was %.*g", __func__, __FILE__, __LINE__, minunit_significant_figures, minunit_tmp_e, minunit_significant_figures, minunit_tmp_r);\
		minunit_status = 1;\
		return;\
	} else {\
		printf("P");\
	}\
)

#define mu_assert_string_eq(expected, result) MU__SAFE_BLOCK(\
	const char* minunit_tmp_e = expected;\
	const char* minunit_tmp_r = result;\
	minunit_assert++;\
	suite_minunit_assert++;\
	if (!minunit_tmp_e) {\
		minunit_tmp_e = "<null pointer>";\
	}\
	if (!minunit_tmp_r) {\
		minunit_tmp_r = "<null pointer>";\
	}\
	if(strcmp(minunit_tmp_e, minunit_tmp_r)) {\
		sprintf(minunit_last_message, "%s failed:\n\t%s:%d: '%s' expected but was '%s'", __func__, __FILE__, __LINE__, minunit_tmp_e, minunit_tmp_r);\
		minunit_status = 1;\
		return;\
	} else {\
		printf("P");\
	}\
)

#endif /* __MINUNIT_H__ */
