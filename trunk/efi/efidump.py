#!/usr/bin/python
# -*- coding: utf-8 -*-
import sys
import struct
import EfiVolume
import EfiFile
import EfiSection
import Guid


def savedata(name, data):
  outstream = open(name, "wb")
  outstream.write(data)
  outstream.close()


instream = open(sys.argv[1])
instream.seek(0, 2)
streamlength = instream.tell()
instream.seek(0, 0)

vid = 0
for v in EfiVolume.find(instream, streamlength):
  print v

  for f in EfiFile.find(instream, v):
    print f
    
    instream.seek(v.Base + v.HeaderLength + f.Base + f.HeaderLength, 0)
    data = instream.read(f.DataLength)
    
    if  (f.Type == f.EFI_FV_FILETYPE_RAW or 
	 f.Type == f.EFI_FV_FILETYPE_SECURITY_CORE):
      savedata("%i-%s" % (vid, f.Guid), data)

    elif f.Type == f.Type == f.EFI_FV_FILETYPE_FFS_PAD:
      pass # ignore padding files

    elif (f.Type == f.EFI_FV_FILETYPE_FREEFORM or
	  f.Type == f.EFI_FV_FILETYPE_PEI_CORE or
	  f.Type == f.EFI_FV_FILETYPE_PXE_CORE or
	  f.Type == f.EFI_FV_FILETYPE_PEIM or
	  f.Type == f.EFI_FV_FILETYPE_DRIVER or
	  f.Type == f.EFI_FV_FILETYPE_COMBINED_PEIM_DRIVER or
	  f.Type == f.EFI_FV_FILETYPE_APPLICATION or
	  f.Type == f.EFI_FV_FILETYPE_FIRMWARE_VOLUME_IMAGE):
      for s in EfiSection.find(f, data):
	print s
	if type(s.Data) == str:
	  savedata("%i-%s.%s" % (vid, f.Guid, s.strsectiontype()), s.Data)
      
    else:
      print "Unknown file type %02x" % f.Type
  vid+=1
