package com.soffid.iam.addons.backup.service;

import com.soffid.iam.addons.backup.common.UserBackup;

import es.caib.seycon.ng.model.UsuariEntity;

public class UserBackupAddonImpl extends UserBackupAddonBase {

	@Override
	protected void handlePerformBackup(Long userId, StringBuffer sb)
			throws Exception {
	}

	@Override
	protected void handleRestoreBackup(UsuariEntity user, UserBackup backup,
			StringBuffer sb) throws Exception {
	}

}
