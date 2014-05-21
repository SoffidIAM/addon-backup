//
// (C) 2013 Soffid
//
//

package com.soffid.iam.addons.backup.model;
/**
 * DAO UserBackupEntity implementation
 */
public class UserBackupEntityDaoImpl extends UserBackupEntityDaoBase
{

	@Override
	public void create(UserBackupEntity entity) {
		if (entity == null)
		{
			throw new IllegalArgumentException(
				"UserBackupEntityDao.update - 'entity' can not be null");
		}
		this.getHibernateTemplate().save(entity);
		
		// NO flush
	}

	@Override
	public void update(UserBackupEntity entity) {
		if (entity == null)
		{
			throw new IllegalArgumentException(
				"UserBackupEntityDao.update - 'entity' can not be null");
		}
		this.getHibernateTemplate().update(entity);
		
		// No flush
	}
}
