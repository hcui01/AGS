################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../lib/boost_1_57_0/thread/src/pthread/once_atomic.cpp \
../lib/boost_1_57_0/thread/src/pthread/thread.cpp 

OBJS += \
./lib/boost_1_57_0/thread/src/pthread/once_atomic.o \
./lib/boost_1_57_0/thread/src/pthread/thread.o 

CPP_DEPS += \
./lib/boost_1_57_0/thread/src/pthread/once_atomic.d \
./lib/boost_1_57_0/thread/src/pthread/thread.d 


# Each subdirectory must supply rules for building sources it contributes
lib/boost_1_57_0/thread/src/pthread/%.o: ../lib/boost_1_57_0/thread/src/pthread/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/cluster/tufts/khardon_group/HaoCUi/boost_1_57_0 -I/cluster/tufts/khardon_group/HaoCUi/mmap-solver/lib -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


