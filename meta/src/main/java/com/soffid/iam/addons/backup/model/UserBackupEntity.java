//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.model;
import com.soffid.iam.model.TenantEntity;
import com.soffid.mda.annotation.*;

@Entity (table="SCX_BACKUP" )
@Depends ({com.soffid.iam.addons.backup.common.UserBackup.class})
public abstract class UserBackupEntity {

	@Column (name="BAC_ID")
	@Nullable
	@Identifier
	public java.lang.Long id;

	@Column (name="BAC_DATE")
	public java.util.Date backupDate;

	@Column (name="BAC_UNTIL")
	@Nullable
	public java.util.Date validUntil;

	@Column (name="BAK_DATA", length=64000)
	public java.lang.String data;

	@Column (name="BAC_USERNAME")
	public java.lang.String userName;

	@Column (name="BAC_ORDER")
	public java.lang.Long order;

	@Nullable
	@Column (name="BAC_TEN_ID")
	public TenantEntity tenant;
	
	@DaoFinder
	public java.util.List<com.soffid.iam.addons.backup.model.UserBackupEntity> findByUser(
		java.lang.String userName) {
	 return null;
	}
	@DaoFinder("select backup\n"
			+ "from com.soffid.iam.addons.backup.model.UserBackupEntity as backup\n"
			+ "where (backup.userName like :userName or :userName is null) and\n"
			+ "           (:date is null or backup.backupDate < :date) and\n"
			+ "           (:date is null or backup.validUntil is null or validUntil > :date) and "
			+ "           tenant.id=:tenantId")
	public java.util.List<com.soffid.iam.addons.backup.model.UserBackupEntity> findByCriteria(
		@Nullable java.lang.String userName, 
		@Nullable java.util.Date date) {
	 return null;
	}
}


@Index (name="SCX_BACKUP_USER_NDX",	unique=false,
	entity=com.soffid.iam.addons.backup.model.UserBackupEntity.class,
	columns={"BAC_USERNAME", "BAC_ORDER"})
class UserIndex {
}
