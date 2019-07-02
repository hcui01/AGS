################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../lib/zchaff/zchaff_base.cpp \
../lib/zchaff/zchaff_dbase.cpp \
../lib/zchaff/zchaff_solver.cpp \
../lib/zchaff/zchaff_utils.cpp 

OBJS += \
./lib/zchaff/zchaff_base.o \
./lib/zchaff/zchaff_dbase.o \
./lib/zchaff/zchaff_solver.o \
./lib/zchaff/zchaff_utils.o 

CPP_DEPS += \
./lib/zchaff/zchaff_base.d \
./lib/zchaff/zchaff_dbase.d \
./lib/zchaff/zchaff_solver.d \
./lib/zchaff/zchaff_utils.d 


# Each subdirectory must supply rules for building sources it contributes
lib/zchaff/%.o: ../lib/zchaff/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/cluster/tufts/khardon_group/HaoCUi/boost_1_57_0 -I/cluster/tufts/khardon_group/HaoCUi/mmap-solver/lib -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


