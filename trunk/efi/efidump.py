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

def dumpsection(s, sid, basefilename, pprefix):
  print "%s%s" % (pprefix, s)

  filename = "%s.S%04i-%s" % (basefilename, sid, s.strsectiontype())
  if type(s.Data) == str:
    savedata(filename, s.Data)

  if s.SubSections:
    sub_sid = 0
    for sub in s.SubSections:
      dumpsection(sub, sub_sid, filename, pprefix + "SUB_")
      sub_sid += 1

instream = open(sys.argv[1])
instream.seek(0, 2)
streamlength = instream.tell()
instream.seek(0, 0)

vid = 0
for v in EfiVolume.find(instream, streamlength):
  print v

  fid = 0
  for f in EfiFile.find(instream, v):
    print f
    
    instream.seek(v.Base + v.HeaderLength + f.Base + f.HeaderLength, 0)
    filedata = instream.read(f.DataLength)
    
    if  (f.Type == f.EFI_FV_FILETYPE_RAW or 
	 f.Type == f.EFI_FV_FILETYPE_SECURITY_CORE):
      savedata("V%04i.F%04i-%s" % (vid, fid, f.Guid), filedata)

    elif f.Type == f.EFI_FV_FILETYPE_FFS_PAD:
      pass # ignore padding files

    elif f.Type == f.EFI_FV_FILETYPE_FIRMWARE_VOLUME_IMAGE:
      print "Warning: FIRMWARE_VOLUME_IMAGE files are currently unsupported"

    elif (f.Type == f.EFI_FV_FILETYPE_FREEFORM or
	  f.Type == f.EFI_FV_FILETYPE_PEI_CORE or
	  f.Type == f.EFI_FV_FILETYPE_PXE_CORE or
	  f.Type == f.EFI_FV_FILETYPE_PEIM or
	  f.Type == f.EFI_FV_FILETYPE_DRIVER or
	  f.Type == f.EFI_FV_FILETYPE_COMBINED_PEIM_DRIVER or
	  f.Type == f.EFI_FV_FILETYPE_APPLICATION):
      sid = 0
      for s in EfiSection.find(filedata):
	dumpsection(s, sid, "V%04i.F%04i-%s" % (vid, fid, f.Guid), "")
	sid += 1

    else:
      print "Unknown file type %02x" % f.Type
    fid+=1
  vid+=1
