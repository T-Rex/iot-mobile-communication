cmake_minimum_required(VERSION 2.6.0)

# We will use the C++11 dialect
set(CMAKE_CXX_FLAGS "-std=c++11" ${CMAKE_CXX_FLAGS})

# Searching for the folder with required header file
find_path(LIBIMOBILEDEVICE_INCLUDE_DIR "libimobiledevice/libimobiledevice.h")

# Searching for the shared library
find_library(LIBIMOBILEDEVICE_LIBRARY_NAMES imobiledevice)

# List of the source files
set(SRCS ios-interop.cpp)

# Result binary file name
set(EXECUTABLE_NAME ios-interop)

# We will build the executable with the specified name from specified source files
add_executable(${EXECUTABLE_NAME} ${SRCS})

# Let compiler know the directories where it should search for header files
target_include_directories(${EXECUTABLE_NAME} PRIVATE ${LIBIMOBILEDEVICE_INCLUDE_DIR})

# Let linker know about what 3rd-party libraries our app depends on
target_link_libraries(${EXECUTABLE_NAME} ${LIBIMOBILEDEVICE_LIBRARY_NAMES})
