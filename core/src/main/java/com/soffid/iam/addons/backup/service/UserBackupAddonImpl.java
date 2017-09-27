package com.soffid.iam.addons.backup.service;

import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.model.UserEntity;

public class UserBackupAddonImpl extends UserBackupAddonBase {

	@Override
	protected void handlePerformBackup(Long userId, StringBuffer sb)
			throws Exception {
	}

	@Override
	protected void handleRestoreBackup(UserEntity user, UserBackup backup,
			StringBuffer sb) throws Exception {
	}

}
