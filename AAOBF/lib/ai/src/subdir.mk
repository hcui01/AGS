################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../lib/ai/src/ai_Factor.cpp \
../lib/ai/src/ai_FactorDense.cpp \
../lib/ai/src/ai_factorgraph.cpp \
../lib/ai/src/ai_graphmodel.cpp \
../lib/ai/src/ai_wmbe.cpp 

OBJS += \
./lib/ai/src/ai_Factor.o \
./lib/ai/src/ai_FactorDense.o \
./lib/ai/src/ai_factorgraph.o \
./lib/ai/src/ai_graphmodel.o \
./lib/ai/src/ai_wmbe.o 

CPP_DEPS += \
./lib/ai/src/ai_Factor.d \
./lib/ai/src/ai_FactorDense.d \
./lib/ai/src/ai_factorgraph.d \
./lib/ai/src/ai_graphmodel.d \
./lib/ai/src/ai_wmbe.d 


# Each subdirectory must supply rules for building sources it contributes
lib/ai/src/%.o: ../lib/ai/src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/cluster/tufts/khardon_group/HaoCUi/boost_1_57_0 -I/cluster/tufts/khardon_group/HaoCUi/mmap-solver/lib -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


