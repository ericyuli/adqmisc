#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>

int main(int argc, char *argv[])
{
	unsigned char buf[5];
	int fd;
	union i2c_smbus_data smbus_data;
	struct i2c_smbus_ioctl_data smbus_ioctl_data;

	
	fd = open("/dev/i2c-0", O_RDWR);
	if (fd < 0) {
		perror("Open i2c bus\n");
		exit(1);
	}
	
	// set slave device to 8051 microprocessor
	if (ioctl(fd, I2C_SLAVE, 0x34)) {
		perror("Set slave i2c address\n");
		exit(1);
	}
	
	// send the initial request
	smbus_data.block[0] = 2; // count of bytes
	smbus_data.block[1] = 0;
	smbus_data.block[2] = 0;
	smbus_ioctl_data.read_write = 0;
	smbus_ioctl_data.command = 0x27;
	smbus_ioctl_data.size = I2C_SMBUS_BLOCK_DATA;
	smbus_ioctl_data.data = &smbus_data;
	if (ioctl(fd, I2C_SMBUS, &smbus_ioctl_data)) {
		perror("Send initial request\n");
		exit(1);
	}
	
	// wait for response
	smbus_data.block[0] = 4;
	smbus_ioctl_data.read_write = 1;
	while(1) {
		if (ioctl(fd, I2C_SMBUS, &smbus_ioctl_data)) {
			perror("Read result\n");
			exit(1);
		}
		if (smbus_data.block[1] == 0x87)
			break;
		usleep(1000);
	}
	if (smbus_data.block[1] != 0x87) {
		fprintf(stderr, "Failed to get response\n");
		exit(1);
	}
	printf("%u\n", (smbus_data.block[3] << 8) | smbus_data.block[2]);
	
	close(fd);
	return 0;
}
