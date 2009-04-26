# -*- coding: utf-8 -*-

from pyftpdlib import ftpserver
import threading
import os

class EFrameFTPHandler(ftpserver.FTPHandler):

    def ftp_SIZE(self, path):
        """Nasty hack since eframe needs SIZE in ASCII mode :("""

        old_type = self.current_type
        self.current_type = 'b'
        ftpserver.FTPHandler.ftp_SIZE(self,path)
        self.current_type = old_type

class EFrameAbstractedFS(ftpserver.AbstractedFS):

    def getsize(self, path):
        """Workaround for eframe org_XXX weirdness."""
        try:
            return os.path.getsize(path)
        except:
            return os.path.getsize(path.replace('org_', ''))

    def isfile(self, path):
        """Workaround for eframe org_XXX weirdness."""
        if os.path.isfile(path):
            return True
        return os.path.isfile(path.replace('org_', ''))

    def open(self, filename, mode):
        """Workaround for eframe org_XXX weirdness."""
        try:
            return open(filename, mode)
        except:
            return open(filename.replace('org_', ''), mode)


class EFrameFTPServer:

    def __init__(self, eframe, ftp_dir):
        self.__eframe = eframe

        authorizer = ftpserver.DummyAuthorizer()
        authorizer.add_user(self.__eframe.ftp_username, self.__eframe.ftp_password, ftp_dir, perm="elr")
        ftp_handler = EFrameFTPHandler
        ftp_handler.authorizer = authorizer
        ftp_handler.abstracted_fs = EFrameAbstractedFS
        self.__ftpd = ftpserver.FTPServer(("0.0.0.0", self.__eframe.ftp_port), ftp_handler)
        self.__ftp_thread = threading.Thread(target=self.__ftpd.serve_forever)
        self.__ftp_thread.setDaemon(True)
        ftpserver.log = self.nolog
        ftpserver.logline = self.nolog
        self.__ftp_thread.start()

    def nolog(self, msg):
        pass

    def Stop(self):
        self.__ftpd.close_all()
