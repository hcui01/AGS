################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../lib/boost_1_57_0/system/src/error_code.cpp 

OBJS += \
./lib/boost_1_57_0/system/src/error_code.o 

CPP_DEPS += \
./lib/boost_1_57_0/system/src/error_code.d 


# Each subdirectory must supply rules for building sources it contributes
lib/boost_1_57_0/system/src/%.o: ../lib/boost_1_57_0/system/src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/cluster/tufts/khardon_group/HaoCUi/boost_1_57_0 -I/cluster/tufts/khardon_group/HaoCUi/mmap-solver/lib -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


