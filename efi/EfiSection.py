# -*- coding: utf-8 -*-
import struct
import Guid
import zlib

class EfiSection:
  
  EFI_SECTION_COMPRESSION		= 0x01
  EFI_SECTION_GUID_DEFINED		= 0x02
  
  EFI_SECTION_PE32			= 0x10
  EFI_SECTION_PIC			= 0x11
  EFI_SECTION_TE			= 0x12
  EFI_SECTION_DXE_DEPEX			= 0x13
  EFI_SECTION_VERSION			= 0x14
  EFI_SECTION_USER_INTERFACE		= 0x15
  EFI_SECTION_COMPATABILITY16		= 0x16
  EFI_SECTION_FIRMWARE_VOLUME_IMAGE	= 0x17
  EFI_SECTION_FREEFORM_SUBTYPE_GUID	= 0x18
  EFI_SECTION_RAW			= 0x19
  EFI_SECTION_PEI_DEPEX			= 0x1b

  def __init__(self, data, filetype):
    self.Data = data
    self.Type = filetype

  def __str__(self):
    result = "EFI_SECTION:\n"
    result += "\tType: 0x%02x (%s)\n" % (self.Type, self.strsectiontype())
    if type(self.Data) == tuple:
      result += "\tData: %s\n" % ', '.join([str(x) for x in self.Data])
    else:
      result += "\tBinary Data Length: 0x%08x\n" % len(self.Data)
    return result

  def strsectiontype(self):
    if self.Type == self.EFI_SECTION_COMPRESSION:
      return "COMPRESSION"
    elif self.Type == self.EFI_SECTION_GUID_DEFINED:
      return "GUID_DEFINED"
    elif self.Type == self.EFI_SECTION_PE32:
      return "PE32"
    elif self.Type == self.EFI_SECTION_PIC:
      return "PIC"
    elif self.Type == self.EFI_SECTION_TE:
      return "TE"
    elif self.Type == self.EFI_SECTION_DXE_DEPEX:
      return "DEPEX"
    elif self.Type == self.EFI_SECTION_VERSION:
      return "VERSION"
    elif self.Type == self.EFI_SECTION_USER_INTERFACE:
      return "USER_INTERFACE"
    elif self.Type == self.EFI_SECTION_COMPATABILITY16:
      return "COMPATABILITY16"
    elif self.Type == self.EFI_SECTION_FIRMWARE_VOLUME_IMAGE:
      return "FIRMWARE_VOLUME_IMAGE"
    elif self.Type == self.EFI_SECTION_FREEFORM_SUBTYPE_GUID:
      return "FREEFORM_SUBTYPE_GUID"
    elif self.Type == self.EFI_SECTION_RAW:
      return "RAW"
    elif self.Type == self.EFI_SECTION_PEI_DEPEX:
      return "PEI_DEPEX"
    return "UNKNOWN"

def find(efifile, efifiledata):
  sections = []

  base = 0
  while(base < efifile.DataLength):

    # align to 4 bytes
    if base % 4:
      base += 4 - (base % 4)
    if base >= efifile.DataLength:
      break

    (length, efitype) = struct.unpack("<3sB", efifiledata[base:base+4])
    length = struct.unpack("<I", length + '\0')[0]
    base += 4
    length -= 4

    data = ()
    if efitype == EfiSection.EFI_SECTION_COMPRESSION:
      (uncomp_length, comp_type) = struct.unpack("<IB", efifiledata[base:base+4+1])
      data = efifiledata[base+5:base+length]
      pass # FIXME: implement decompression

    elif efitype == EfiSection.EFI_SECTION_GUID_DEFINED:
      # FIXME: implement properly
      data = efifiledata[base:base+length]
      pass

    elif efitype == EfiSection.EFI_SECTION_VERSION:
      (build_number, ) = struct.unpack("<H", efifiledata[base:base+2])
      string = unicode(efifiledata[base+2:base+length-2], "utf-16")
      data = (build_number, string)

    elif efitype == EfiSection.EFI_SECTION_USER_INTERFACE:
      data = (unicode(efifiledata[base:base+length-2], "utf-16"), )

    elif efitype == EfiSection.EFI_SECTION_FREEFORM_SUBTYPE_GUID:
      data = (Guid.strguid(efifiledata[base:base+length]), )

    else:
      data = efifiledata[base:base+length]

    sections.append(EfiSection(data, efitype))

    base += length

  return sections
