package Entidades;

import java.util.Map;

public class retornoMasterFile
{
	private String dirDnsAux;
	private Map<String, String> zonas;
	private String dirMailBoxEncharge;
	private int versionMasterFile;
	private long refreshInterval;
	private long expireInterval;
	private long TTL;
	
	public String getDirDnsAux()
	{
		return dirDnsAux;
	}
	public void setDirDnsAux(String dirDnsAux)
	{
		this.dirDnsAux = dirDnsAux;
	}
	public Map<String, String> getZonas()
	{
		return zonas;
	}
	public void setZonas(Map<String, String> zonas)
	{
		this.zonas = zonas;
	}
	public String getDirMailBoxEncharge()
	{
		return dirMailBoxEncharge;
	}
	public void setDirMailBoxEncharge(String dirMailBoxEncharge)
	{
		this.dirMailBoxEncharge = dirMailBoxEncharge;
	}
	public int getVersionMasterFile()
	{
		return versionMasterFile;
	}
	public void setVersionMasterFile(int versionMasterFile)
	{
		this.versionMasterFile = versionMasterFile;
	}
	public long getRefreshInterval()
	{
		return refreshInterval;
	}
	public void setRefreshInterval(long refreshInterval)
	{
		this.refreshInterval = refreshInterval;
	}
	public long getExpireInterval()
	{
		return expireInterval;
	}
	public void setExpireInterval(long expireInterval)
	{
		this.expireInterval = expireInterval;
	}
	public long getTTL()
	{
		return TTL;
	}
	public void setTTL(long tTL)
	{
		TTL = tTL;
	}
	
}
