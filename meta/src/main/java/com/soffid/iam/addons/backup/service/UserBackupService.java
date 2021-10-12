//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.service;
import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.api.AsyncList;
import com.soffid.iam.api.PagedResult;
import com.soffid.iam.service.AsyncRunnerService;
import com.soffid.mda.annotation.*;

import es.caib.seycon.ng.model.AccountEntity;

import org.springframework.transaction.annotation.Transactional;

@Service ( translatedName="UserBackupService",
	 translatedPackage="com.soffid.iam.addons.backup.service")
@Depends ({com.soffid.iam.addons.backup.model.UserBackupEntity.class,
	es.caib.seycon.ng.model.UsuariEntity.class,
	AccountEntity.class,
	es.caib.seycon.ng.servei.UsuariService.class,
	es.caib.seycon.ng.servei.AplicacioService.class,
	es.caib.seycon.ng.servei.DadesAddicionalsService.class,
	es.caib.seycon.ng.servei.GrupService.class,
	es.caib.seycon.ng.servei.AccountService.class,
	com.soffid.iam.addons.backup.service.UserBackupAddon.class,
	es.caib.seycon.ng.servei.ConfiguracioService.class,
	AsyncRunnerService.class,
	es.caib.seycon.ng.servei.DispatcherService.class})
public class UserBackupService {

	@Transactional(rollbackFor={java.lang.Exception.class})
	public void performBackup(
		java.lang.String userId)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}

	@Transactional(rollbackFor={java.lang.Exception.class})
	public void performBackup(
		java.lang.String accountId, String system)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}

	@Operation ( grantees={com.soffid.iam.addons.backup.backup_restore.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public void restoreBackup(
		com.soffid.iam.addons.backup.common.UserBackup backup)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}
	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public java.util.List<com.soffid.iam.addons.backup.common.UserBackup> findBackup(
		@Nullable java.lang.String user, 
		@Nullable java.util.Date date, 
		@Nullable java.lang.String freeText)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	 return null;
	}
	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public java.util.List<com.soffid.iam.addons.backup.common.UserBackup> getUserBackups(
		java.lang.Long userId)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	 return null;
	}
	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public com.soffid.iam.addons.backup.common.UserBackupConfig getConfig()
		throws es.caib.seycon.ng.exception.InternalErrorException {
	 return null;
	}
	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public java.lang.String getBackupContent(
		com.soffid.iam.addons.backup.common.UserBackup backup)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	 return null;
	}

	@Operation ( grantees={com.soffid.iam.addons.backup.backup_configure.class})
	@Transactional(rollbackFor={java.lang.Exception.class})
	public void setConfig(
		com.soffid.iam.addons.backup.common.UserBackupConfig config)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}
	
	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	public PagedResult<UserBackup> findUserBackupByJsonQuery(@Nullable String query, @Nullable Integer first, @Nullable Integer pageSize) {return null;}

	@Operation ( grantees={com.soffid.iam.addons.backup.backup_query.class})
	public AsyncList<UserBackup> findUserBackupByJsonQueryAsync(@Nullable String query) {return null;}
}
