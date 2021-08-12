package com.southgis.ibase.taskManagement.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.southgis.ibase.taskManagement.dto.DagNodeValueDto;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;

public class FtpUtil {
    private static Logger logger = LoggerFactory.getLogger(FtpUtil.class);
    public FTPClient ftp;
    public ArrayList<String> arFiles;
    
    /**
     * 重载构造函数
     * @param isPrintCommmand 是否打印与FTPServer的交互命令
     */
    public FtpUtil(boolean isPrintCommmand){
        ftp = new FTPClient();
        arFiles = new ArrayList<String>();
        if(isPrintCommmand){
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        }
    }

    /**
     * 登陆FTP服务器
     * @param host FTPServer IP地址
     * @param port FTPServer 端口
     * @param username FTPServer 登陆用户名
     * @param password FTPServer 登陆密码
     * @return 是否登录成功
     * @throws IOException
     */
    public boolean login(String host,int port,String username,String password) throws IOException {
        this.ftp.connect(host,port);
        if(FTPReply.isPositiveCompletion(this.ftp.getReplyCode())){
            if(this.ftp.login(username, password)){
                if (FTPReply.isPositiveCompletion(ftp.sendCommand("OPTS UTF8", "ON"))) {
                    this.ftp.setControlEncoding("UTF-8");
                }else{
                    this.ftp.setControlEncoding("GBK");
                }

                return true;
            }
        }
        if(this.ftp.isConnected()){
            this.ftp.disconnect();
        }
        return false;
    }

    /**
     * 关闭数据链接
     * @throws IOException
     */
    public void disConnection() throws IOException{
        if(this.ftp.isConnected()){
            this.ftp.disconnect();
        }
    }

    /**
     * 递归遍历出目录下面所有文件
     * @param pathName 需要遍历的目录，必须以"/"开始和结束
     * @throws IOException
     */
    public void pathList(String pathName) throws IOException{
        if(pathName.startsWith("/")&&pathName.endsWith("/")){
            String directory = pathName;
            //更换目录到当前目录
            this.ftp.changeWorkingDirectory(directory);
            FTPFile[] files = this.ftp.listFiles();
            for(FTPFile file:files){
                if(file.isFile()){
                    arFiles.add(directory+file.getName());
                }else if(file.isDirectory()){
                    pathList(directory+file.getName()+"/");
                }
            }
        }
    }

    public void pathForDagNodeValueDtoList(String pathName,List<DagNodeValueDto> dagNodeValueDtoList) throws IOException{
        if(pathName.startsWith("/")&&pathName.endsWith("/")){
            String directory = pathName;
            //更换目录到当前目录
            this.ftp.changeWorkingDirectory(directory);
            FTPFile[] files = this.ftp.listFiles();
            for(FTPFile file:files){
                if(file.isFile()){
                    //arFiles.add(directory+file.getName());
                    DagNodeValueDto dagNodeValueDto = new DagNodeValueDto();
                    //dagNodeValueDto.set
                 //   dagNodeValueDtoList
                }else if(file.isDirectory()){
                    pathList(directory+file.getName()+"/");
                }
            }
        }
    }

    /**
     * 递归遍历目录下面指定的文件名
     * @param pathName 需要遍历的目录，必须以"/"开始和结束
     * @param ext 文件的扩展名
     * @throws IOException
     */
    public void pathList(String pathName,String ext) throws IOException{
        if(pathName.startsWith("/")&&pathName.endsWith("/")){
            String directory = pathName;
            //更换目录到当前目录
            this.ftp.changeWorkingDirectory(directory);
            FTPFile[] files = this.ftp.listFiles();
            for(FTPFile file:files){
                if(file.isFile()){
                    if(file.getName().endsWith(ext)){
                        arFiles.add(directory+file.getName());
                    }
                }else if(file.isDirectory()){
                    pathList(directory+file.getName()+"/",ext);
                }
            }
        }
    }

    /**
     * 遍历出目录下面所有文件,不包括文件夹
     * @param pathName 需要遍历的目录，必须以"/"开始和结束
     * @throws IOException
     */
    public void pathListWithoutDirectory(String pathName) throws IOException{
        if(pathName.startsWith("/")&&pathName.endsWith("/")){
            String directory = pathName;
            //更换目录到当前目录
            this.ftp.changeWorkingDirectory(directory);
            FTPFile[] files = this.ftp.listFiles();
            for(FTPFile file:files){
                if(file.isFile()){
                    arFiles.add(directory+file.getName());
                }
            }
        }
    }

    /**
	 * ftp文件上传
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 * @param path 相对路径
	 * @param fileName 文件名
	 * @param fileSteam 文件流
	 * @throws Exception 
	 */
	public static Boolean hutoolUploadFile(String host, int port, String username, String password, 
			String path, String fileName, InputStream fileSteam, FtpMode ftpMode) throws Exception  {
		
		Boolean result = false;
		
		try ( Ftp ftp = new Ftp(host, port, username, password, CharsetUtil.CHARSET_UTF_8, ftpMode); ){
			result = ftp.upload(path, fileName, fileSteam );
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("上传文件失败:" + e.getMessage());
		}
		return result;
	}

	/**
	 * ftp文件下载
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 * @param remotePath 文件目录
	 * @param fileName 文件名
	 * @param FtpModel ftp连接模式
	 * @return
	 */
	public static ByteArrayOutputStream hutoolDownloadFile(String host, Integer port, String username,
			String password, String remotePath, String fileName, FtpMode ftpModel) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try ( Ftp ftp = new Ftp(host, port, username, password, CharsetUtil.CHARSET_UTF_8 ,ftpModel); ){
			ftp.download(remotePath, fileName, out);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return out;
	}

	public static Long findFileLastDDLtime(String host, Integer port, String username, String password, String path,
			String fileName) {
		Long result = 0L;
		try ( Ftp ftp = new Ftp(host, port, username, password, CharsetUtil.CHARSET_UTF_8); ){
			int lastIndex = fileName.lastIndexOf("/");
			String realPath = fileName.substring(0,lastIndex);
			String realName = fileName.substring(lastIndex+1,fileName.length());
			FTPFile[] files = ftp.lsFiles(realPath);
			List<Long> times = Stream.of(files).filter(f -> f.getName().equals(realName)).map(f -> DateUtil.date(f.getTimestamp()).getTime()).collect(Collectors.toList());
			if(null!=times && times.size()>0){
				result = times.get(0);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}
    
//   public static void main(String[] args) throws IOException {
//	   String host = "192.168.10.171";
//	   Integer port = 21;
//	   String username = "ftptest";
//	   String password = "ftptest";
//	   String path = "/";
//	   String fileName = "/fpa/QualityInspection/data/2019/440103荔湾区.gdb";
//	   findFileLastDDLtime(host, port, username, password, path, fileName);
//   }
}
