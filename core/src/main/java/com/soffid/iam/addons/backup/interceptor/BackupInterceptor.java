package com.soffid.iam.addons.backup.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.SessionFactory;

import com.soffid.iam.addons.backup.service.UserBackupService;
import com.soffid.iam.model.TaskEntity;
import com.soffid.iam.sync.engine.TaskHandler;

public class BackupInterceptor implements Interceptor, MethodInterceptor
{
	private SessionFactory sessionFactory = null;
	private UserBackupService userBackupService = null;



	public UserBackupService getUserBackupService() {
		return userBackupService;
	}

	public void setUserBackupService(UserBackupService userBackupservice) {
		this.userBackupService = userBackupservice;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public Object invoke(MethodInvocation mi) throws Throwable {
		Object result = mi.proceed();
		Method method = mi.getMethod();
		if (method.getParameterTypes().length == 1 &&
			! (mi.getArguments()[0] instanceof Long))
		{
			Object param = mi.getArguments()[0];
			if (method.getName().equals("create") && param instanceof TaskEntity)
			{
				TaskEntity tasque = (TaskEntity) param;
				if (tasque.getTransaction().equals(TaskHandler.UPDATE_USER) && tasque.getUser() != null)
				{
					userBackupService.performBackup(tasque.getUser());
				}
				if (tasque.getTransaction().equals(TaskHandler.UPDATE_ACCOUNT) && tasque.getUser() != null && tasque.getSystemName() != null)
				{
					userBackupService.performBackup(tasque.getUser(), tasque.getSystemName());
				}
			}
		}
		return result;
	}
}
