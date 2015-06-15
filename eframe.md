This code implements the protocol for controlling a BT Eframe 1000 from python. It requires [pyftpdlib](http://code.google.com/p/pyftpdlib/).

The code is available [here](http://code.google.com/p/adqmisc/source/browse/trunk/eframe)

Utilities:
|copymusic|Copies mp3s from `<ftpdir>`/vCopyFolder to the eframe|
|:--------|:----------------------------------------------------|
|copyphotos|Copies jpgs from `<ftpdir>`/vCopyFolder to the eframe|
|copyrss  |Copies the file `<ftpdir>`/vRssFolder/PF110.RSS to the eframe|
|encoderss|Encodes a plaintext list of RSS feeds into the binary PF110.RSS file. See samplerssfeeds.txt for an example|