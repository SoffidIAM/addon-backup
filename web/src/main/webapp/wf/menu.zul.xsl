<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:zul="http://www.zkoss.org/2005/zul">

	<xsl:template match="/zul:zk/zul:zscript[1]" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
		<zul:zscript>
			boolean canConfigureBackup = es.caib.seycon.ng.utils.Security.isUserInRole("backup:configure/*");
			boolean canRestoreBackup = es.caib.seycon.ng.utils.Security.isUserInRole("backup:restore/*");
		</zul:zscript>
	</xsl:template>
	
	<xsl:template match="zul:tree/zul:treechildren/zul:treeitem[3]/zul:treechildren/zul:treeitem[8]" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
			
			<zul:treeitem>
				<zul:treerow>
					<zul:apptreecell langlabel="backup.configure.menu" 
								pagina="addon/backup/configure.zul">
							<xsl:attribute name="if">${canConfigureBackup || canRestoreBackup}</xsl:attribute>
					</zul:apptreecell>
				</zul:treerow>
			</zul:treeitem>
		</xsl:template>
 

	<xsl:template match="node()|@*" priority="2">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>