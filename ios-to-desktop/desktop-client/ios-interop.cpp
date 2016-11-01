#include <stdio.h>
#include <string.h>
#include <libimobiledevice/libimobiledevice.h>

#include <vector>
#include <string>
#include <sstream>

#define IOS_INTEROP_PORT 	8080
#define IOS_INTEROP_BUFFER_SIZE	128

struct split_params
{
  enum empties_t { empties_ok, no_empties };
};

template <typename Container>
Container& split(
  Container&                                 result,
  const typename Container::value_type&      s,
  typename Container::value_type::value_type delimiter,
  split_params::empties_t                    empties = split_params::empties_ok )
{
  result.clear();
  std::istringstream ss( s );
  while (!ss.eof())
  {
    typename Container::value_type field;
    getline( ss, field, delimiter );
    if ((empties == split_params::no_empties) && field.empty()) continue;
    result.push_back( field );
  }
  return result;
}

template <typename T>
T lexical_cast(const std::string& str)
{
    T var;
    std::istringstream iss;
    iss.str(str);
    iss >> var;
    // deal with any error bits that may have been set on the stream
    return var;
}

int main(int argc, const char ** argv)
{
	do
	{
		int count = 0;
		char ** device_names = nullptr;
		idevice_error_t error_code = IDEVICE_E_UNKNOWN_ERROR;
		//-----------------------------------
		printf("Obtaining the device list...\n");
		if((error_code = idevice_get_device_list(&device_names, &count)) != IDEVICE_E_SUCCESS)
		{
			printf("Unable to get the device list (%i).\n", error_code);
			break;
		}
		printf("Available devices:\n");
		for(int i = 0; i < count; ++i)
		{
			printf("\t%i: %s\n", i, device_names[i]);
		}
		int device_index = -1;
		printf("Enter the device index: ");
		scanf("%i", &device_index);
		//-----------------------------------
		printf("Creating the device %i...\n", device_index);
		idevice_t device;
		if((error_code = idevice_new(&device, device_names[device_index])) != IDEVICE_E_SUCCESS)
		{
			printf("Unable to create the device (%i), error %i.\n", device_index, error_code);
			break;
		}
		idevice_device_list_free(device_names);
		printf("Device %i was created.\n", device_index);
		//-----------------------------------
		printf("Connecting to the device.\n");
		idevice_connection_t connection;
		if((error_code = idevice_connect(device, IOS_INTEROP_PORT, &connection)) != IDEVICE_E_SUCCESS)
		{
			idevice_free(device);
			printf("Unable to connect. Error %i\n", error_code);
			break;
		} 
		printf("Connected to the port %i\n", IOS_INTEROP_PORT);
		//-----------------------------------
		printf("Receiving the task.\n");
		char buffer[IOS_INTEROP_BUFFER_SIZE];
		memset(buffer, 0, IOS_INTEROP_BUFFER_SIZE);
		uint32_t buffer_length = 0;
		if((error_code = idevice_connection_receive(
			connection, buffer, IOS_INTEROP_BUFFER_SIZE-1, &buffer_length)) != IDEVICE_E_SUCCESS)
		{
			idevice_disconnect(connection);
			idevice_free(device);
			printf("Unable to receive the task. Error %i\n", error_code);
			break;
		}
		//-----------------------------------
		printf("Parsing the task.\n");
		std::string message(buffer);
		std::vector<std::string> arguments;
		split(arguments, message, ';');
		for(size_t i = 0; i < arguments.size(); ++i)
		{
			printf("%zu: %s\n", i, arguments[i].c_str());
		}
		if(arguments.size() == 3 && arguments[0] == "add")
		{
			uint32_t bytes_sent = 0;
			int a = lexical_cast<int>(arguments[1]);
			int b = lexical_cast<int>(arguments[2]);
			int result = a + b;
			std::string response = std::string("add_result;") + std::to_string(result);
			idevice_connection_send(connection, response.c_str(), response.size(), &bytes_sent);
			printf("Bytes sent: %d\n", bytes_sent);
		}
		idevice_disconnect(connection);
		idevice_free(device);
		getchar();
	}
	while(false);
	return 0;
}
