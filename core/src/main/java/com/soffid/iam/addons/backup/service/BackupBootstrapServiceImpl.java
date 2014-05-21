package com.soffid.iam.addons.backup.service;

import java.io.File;

import com.soffid.iam.addons.backup.common.UserBackupConfig;

public class BackupBootstrapServiceImpl extends BackupBootstrapServiceBase {

	@Override
	protected void handleConsoleBoot() throws Exception {
		UserBackupService svc = getUserBackupService();
		UserBackupConfig cfg = svc.getConfig();
		boolean anyChange = false;
		if (cfg.getFullBackupHour() == null)
		{
			cfg.setFullBackupHour(new Integer(0));
			anyChange = true;
		}
		if (cfg.getFullBackupMinute() == null)
		{
			cfg.setFullBackupMinute(new Integer(0));
			anyChange = true;
		}
		if (cfg.getUserBackupCopies() == null)
		{
			cfg.setUserBackupCopies(new Integer(10));
			anyChange = true;
		}
		if (cfg.getUserBackupDelay() == null)
		{
			cfg.setUserBackupDelay(new Long(600)); // 10 minutes
			anyChange = true;
		}		
		if (cfg.getFullBackupDir() == null)
		{
			File f = new File (System.getProperty("jboss.home.dir"));
			f = f.getParentFile();
			f = new File (f, "backup");
			cfg.setFullBackupDir(f.getAbsolutePath());
			anyChange =true;
		}

		if (anyChange)
		{
			svc.setConfig(cfg);
		}
	}

	@Override
	protected void handleSyncServerBoot() throws Exception {
	}

}
