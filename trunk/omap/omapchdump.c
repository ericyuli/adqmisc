#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <string.h>
#include <utils.h>

struct omap_ch_header
{
  uint32_t offset;
  uint32_t size;
  uint32_t unused1;
  uint32_t unused2;
  uint32_t unused3;
  char name[12];
};

int main(int argc, char *argv[])
{
  struct omap_ch_header header;
  int fd;
  int outfd;
  char name[32];
  char tmp[256];
  int idx;
  off_t oldpos;
  
  if (argc != 2) {
    fprintf(stderr, "Syntax: omapdump <filename>\n");
    exit(1);
  }
  
  fd = open(argv[1], O_RDONLY);
  if (fd  < 0) {
    fprintf(stderr, "Could not open %s\n", argv[1]);
    exit(1);
  }
  
  idx = 0;
  while(1) {
    if (read(fd, &header, sizeof(header)) != sizeof(header))
      break;
    header.offset = le32_to_cpu(header.offset);
    header.size = le32_to_cpu(header.size);
    if (header.offset == 0xffffffff)
      break;

    memset(name, 0, sizeof(name));
    strncpy(name, header.name, sizeof(header.name));
    printf("Dumping %s\n", name);
    sprintf(tmp, "%i.%s", idx, name);
    
    oldpos = lseek(fd, 0, SEEK_CUR);
    lseek(fd, header.offset, 0);
    
    outfd = open(tmp, O_CREAT | O_TRUNC | O_WRONLY);
    while(header.size) {
      int rsize = sizeof(tmp);
      if (rsize > header.size)
        rsize = header.size;
        
      rsize = read(fd, tmp, rsize);
      if (rsize < 0)
        break;
        
      write(outfd, tmp, rsize);
      header.size -= rsize;
    }
    close(outfd);
    lseek(fd, oldpos, 0);
    idx++;
  }
  
  close(fd);
}