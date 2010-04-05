# -*- coding: utf-8 -*-
import struct 

def strguid(raw):
  return "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x" % struct.unpack("<IHH8B", raw)
