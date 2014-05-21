package com.soffid.addons.backup.test;

import java.util.Collection;

import org.jfree.util.Log;

import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.addons.backup.common.UserBackupConfig;
import com.soffid.iam.addons.backup.service.UserBackupService;
import com.soffid.test.AbstractHibernateTest;

import es.caib.seycon.ng.comu.Account;
import es.caib.seycon.ng.comu.AccountType;
import es.caib.seycon.ng.comu.Aplicacio;
import es.caib.seycon.ng.comu.AutoritzacioRol;
import es.caib.seycon.ng.comu.Configuracio;
import es.caib.seycon.ng.comu.Dispatcher;
import es.caib.seycon.ng.comu.Domini;
import es.caib.seycon.ng.comu.DominiContrasenya;
import es.caib.seycon.ng.comu.DominiUsuari;
import es.caib.seycon.ng.comu.Grup;
import es.caib.seycon.ng.comu.Maquina;
import es.caib.seycon.ng.comu.OsType;
import es.caib.seycon.ng.comu.PoliticaContrasenya;
import es.caib.seycon.ng.comu.Rol;
import es.caib.seycon.ng.comu.RolAccount;
import es.caib.seycon.ng.comu.TipusDada;
import es.caib.seycon.ng.comu.TipusDominiUsuariEnumeration;
import es.caib.seycon.ng.comu.TipusUsuari;
import es.caib.seycon.ng.comu.UserAccount;
import es.caib.seycon.ng.comu.Usuari;
import es.caib.seycon.ng.comu.UsuariGrup;
import es.caib.seycon.ng.comu.Xarxa;
import es.caib.seycon.ng.exception.AccountAlreadyExistsException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.exception.NeedsAccountNameException;
import es.caib.seycon.ng.servei.AccountService;
import es.caib.seycon.ng.servei.AplicacioService;
import es.caib.seycon.ng.servei.ApplicationBootService;
import es.caib.seycon.ng.servei.AutoritzacioService;
import es.caib.seycon.ng.servei.ConfiguracioService;
import es.caib.seycon.ng.servei.DadesAddicionalsService;
import es.caib.seycon.ng.servei.DispatcherService;
import es.caib.seycon.ng.servei.DominiUsuariService;
import es.caib.seycon.ng.servei.GrupService;
import es.caib.seycon.ng.servei.InternalPasswordService;
import es.caib.seycon.ng.servei.InternalPasswordServiceImpl;
import es.caib.seycon.ng.servei.PasswordService;
import es.caib.seycon.ng.servei.PuntEntradaService;
import es.caib.seycon.ng.servei.SeyconServiceLocator;
import es.caib.seycon.ng.servei.UsuariService;
import es.caib.seycon.ng.servei.XarxaService;
import es.caib.seycon.ng.utils.Security;

public class BackupTest extends AbstractHibernateTest {
	
	protected InternalPasswordServiceImpl ps;
	protected ConfiguracioService configSvc;
	protected AplicacioService appSvc;
	protected DominiUsuariService dominiSvc;
	protected DispatcherService dispatcherSvc;
	protected GrupService grupSvc;
	protected XarxaService xarxaSvc;
	protected UsuariService usuariSvc;
	protected PasswordService passSvc;
	protected AutoritzacioService autSvc;
	protected DadesAddicionalsService tdSvc;
	protected PuntEntradaService peSvc;
	protected AccountService accountSvc;
	protected InternalPasswordService internalPassSvc;
	protected UserBackupService backupSvc;

	public void setupdb() throws InternalErrorException,
			NeedsAccountNameException
	{ 
		SeyconServiceLocator.instance().init("testBeanRefFactory.xml", "beanRefFactory"); //$NON-NLS-1$ //$NON-NLS-2$

		configSvc = (ConfiguracioService) context.getBean(ConfiguracioService.SERVICE_NAME);
		appSvc = (AplicacioService) context.getBean(AplicacioService.SERVICE_NAME);
		grupSvc = (GrupService) context.getBean(GrupService.SERVICE_NAME);
		dominiSvc = (DominiUsuariService) context.getBean(DominiUsuariService.SERVICE_NAME);
		dispatcherSvc = (DispatcherService) context.getBean(DispatcherService.SERVICE_NAME);
		xarxaSvc = (XarxaService) context.getBean(XarxaService.SERVICE_NAME);
		usuariSvc = (UsuariService) context.getBean(UsuariService.SERVICE_NAME);
		internalPassSvc = (InternalPasswordService) context.getBean(InternalPasswordService.SERVICE_NAME);
		autSvc = (AutoritzacioService) context.getBean(AutoritzacioService.SERVICE_NAME);
		tdSvc = (DadesAddicionalsService) context.getBean(DadesAddicionalsService.SERVICE_NAME);
		peSvc = (PuntEntradaService) context.getBean(PuntEntradaService.SERVICE_NAME);
		accountSvc = (AccountService) context.getBean(AccountService.SERVICE_NAME);
		passSvc = (PasswordService) context.getBean(PasswordService.SERVICE_NAME);
		backupSvc = (UserBackupService) context.getBean(UserBackupService.SERVICE_NAME);

		ApplicationBootService startupSvc = (ApplicationBootService) context.getBean(ApplicationBootService.SERVICE_NAME);
		startupSvc.consoleBoot();

	
		System.setProperty("soffid.ui.maxrows", "9999");

        	
	}

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	
		Security.nestedLogin("Test", new String[] { 
				Security.AUTO_AUTHORIZATION_ALL });
		try {
			setupdb();
		} finally {
			Security.nestedLogoff();
		}
	
	}

	public void testBackup () throws InternalErrorException
	{	
		Security.nestedLogin("Test", new String[] { 
				Security.AUTO_AUTHORIZATION_ALL });
		try {
			UserBackupConfig backupCfg = backupSvc.getConfig();
			backupCfg.setUserBackupDelay(0L);
			backupSvc.setConfig(backupCfg);
			System.out.println ("Updating admin user");
			Log.info("Updateing admin user");
			Usuari usu = usuariSvc.findUsuariByCodiUsuari("admin");
			usu.setSegonLlinatge("Segon llinatge");
			usuariSvc.update(usu);
			
			listUserBackups(usu);
			
			UserBackup backup = backupSvc.getUserBackups(usu.getId()).get(0);
			
			backupSvc.restoreBackup(backup);
			usu = usuariSvc.findUsuariByCodiUsuari("admin");
			System.out.println("2nd sn="+usu.getSegonLlinatge());
			
			// Add a new role and grant
			Rol rol = new Rol();
			rol.setCodiAplicacio("SOFFID");
			rol.setBaseDeDades("soffid"); //$NON-NLS-1$
			rol.setContrasenya(new Boolean(false));
			rol.setDefecte(new Boolean(true));
			rol.setDescripcio("Soffid test role"); //$NON-NLS-1$
			rol.setGestionableWF(new Boolean(false));
			rol.setNom("SOFFID_TEST"); //$NON-NLS-1$
			rol.setDomini(new Domini());
			rol = appSvc.create(rol);
			
			Account account = accountSvc.findAccount("admin", "soffid");
			
			RolAccount ru = new RolAccount();
			ru.setBaseDeDades(rol.getBaseDeDades());
			ru.setCodiAplicacio("SOFFID");
			ru.setCodiUsuari(usu.getCodi());
			ru.setNomRol(rol.getNom());
			ru.setAccountName("admin");
			ru.setAccountDispatcher("soffid");
			ru.setAccountId(account.getId());
			appSvc.create(ru);
			
			grupSvc.addGrupToUsuari("admin", "enterprise");
		
			System.out.println ("=============== ACCOUNT STATUS ============== ");
			listRoles (account);
			listGroups (usu);

			UserBackup ub = backupSvc.getUserBackups(usu.getId()).get(1);
			backupSvc.restoreBackup(ub);
			
			System.out.println ("=============== AFTER 1ST RESTORE ============== ");
			listRoles (account);
			listGroups (usu);
			
			UserBackup ub2 = backupSvc.getUserBackups(usu.getId()).get(1);
			backupSvc.restoreBackup(ub2);

			System.out.println ("=============== AFTER 2ND RESTORE ============== ");
			listRoles (account);
			listGroups (usu);

			// Adding secondary group
			int rows = backupSvc.findBackup(null, null, null).size();
			System.out.println ("Finder finds "+rows+" rows");
		} finally {
			Security.nestedLogoff();
		}
	}

	private void listGroups(Usuari usu) throws InternalErrorException {
		System.out.println ("=========== GROUPS for "+usu.getId()+" ==============================");
		for (UsuariGrup ur: grupSvc.findUsuariGrupsByCodiUsuari(usu.getCodi()) )
		{
			System.out.println ("Grup :"+ur.getCodiGrup());
		}
	}

	private void listRoles(Account account) throws InternalErrorException {
		System.out.println ("=========== Roles for "+account.getId()+" ==============================");
		for (RolAccount ur: appSvc.findRolAccountByAccount(account.getId()) )
		{
			System.out.println ("Rol :"+ur.getNomRol());
		}
	}

	private void listUserBackups(Usuari usu) throws InternalErrorException {
		System.out.println ("=========================================");
		for ( UserBackup backup: backupSvc.getUserBackups(usu.getId()))
		{
			System.out.println ("Backup "+backup.getId());
			System.out.println (backupSvc.getBackupContent(backup));
		}
	}

}
