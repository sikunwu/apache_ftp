public class AssetCatalogManagementService {
   
    /**
     * 创建ftp连接
     *
     * @param ip       IP地址
     * @param port     端口
     * @param username 用户名
     * @param password 密码
     */
    private static FTPClient ftpConnection(String ip, int port, String username, String password) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ip, port);
            ftpClient.login(username, password);
            //是否成功登录服务器
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();
                log.error("ftp连接失败");
                System.exit(1);
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
        } catch (IOException e) {
            log.error("ftp连接失败", e);
        }
        return ftpClient;
    }

    /**
     * 关闭ftp连接
     *
     * @param ftpClient ftp连接对象
     */
    private static void close(FTPClient ftpClient) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                log.error("ftp关闭连接失败", e);
            }
        }
    }


    @Override
    public List<FtpTreeVo> getFtpTreeByRoute(String rid, String path) {
        if (CheckUtil.isNullorEmpty(path)) {
            path = "/";
        }
        //通过数据源id获取数据源信息
        DataSourceInfo dataSourceInfoDto = datasourceService.findDataSourceInfoByRid(rid);
        ExceptionAssertEnum.ServiceError.throwIsNull(dataSourceInfoDto, "找不到相应的数据源信息");
        ExceptionAssertEnum.ServiceError.throwIsFalse(dataSourceInfoDto.getDataSourceType() == DataSourceType.FTP_DATASOURCE, "非FTP数据源");

        DagNodeValueDto ftpOfDagNodeValueDtoByDataSourceInfo = findFtpOfDagNodeValueDtoByDataSourceInfo(dataSourceInfoDto);
        return null;
       
    }

    public NodeValueDto findFtpOfDagNodeValueDtoByDataSourceInfo(DataSourceInfo dataSourceInfo) {
        FtpUtil f = new FtpUtil(false);
        DagNodeValueDto dagNodeValueDto = new DagNodeValueDto();
        try {
            f.login(dataSourceInfo.getServerHost(),Integer.valueOf(dataSourceInfo.getServerPort()),dataSourceInfo.getDataBaseUserName(),dataSourceInfo.getDataBasePassWord());
            pathForDagNodeValueDtoList(dataSourceInfo.getDataParams(),dagNodeValueDto,f.ftp);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                f.disConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dagNodeValueDto;
    }

    public void pathForDagNodeValueDtoList(String pathName,DagNodeValueDto dagNodeValueDto,FTPClient ftp) throws IOException {
        if (pathName.startsWith("/") && pathName.endsWith("/")) {
            String directory = pathName;
            dagNodeValueDto.setValue(directory);
            dagNodeValueDto.setLabel(directory);
            //更换目录到当前目录
            ftp.enterLocalPassiveMode();
            ftp.changeWorkingDirectory(new String(directory.getBytes("utf-8"), FTP.DEFAULT_CONTROL_ENCODING));
            FTPFile[] files = ftp.listFiles();

            for (FTPFile file : files) {
                DagNodeValueDto dnvd = new DagNodeValueDto();
                dnvd.setLabel(file.getName());
                dnvd.setValue(pathName+file.getName());
                dagNodeValueDto.getChildren().add(dnvd);
                dnvd.setCanCheck(true);
//                if (file.isFile()) {
//                    dnvd.setCanCheck(true);
//                }
                if (file.isDirectory()) {
                    pathForDagNodeValueDtoList(directory + file.getName() + "/",dnvd,ftp);
                }
            }
        }
    }


    /***
     *
     * 获取下一级的Ftp文件
     * @param path 当前请求路径
     * @param ftpClient Ftp连接客户端
     * @return List<Map < String, Object>>
     */
    private List<Map<String, Object>> getLocalFtpFile(String path, FTPClient ftpClient) {
        List<Map<String, Object>> mapList = new ArrayList<>();

        //shp或者tif文件的名字
        Set<String> set = new HashSet<>();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles();
            for (FTPFile ftpFile : ftpFiles) {
                String name = ftpFile.getName();
                boolean suffix = name.endsWith(".shp") || name.endsWith(".TIF");
                if (suffix && !set.contains(name.substring(0, name.indexOf(".")))) {
                    set.add(name.substring(0, name.indexOf(".")));
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", path + name);
                    map.put("type", 0);
                    map.put("label", name);
                    mapList.add(map);
                }
            }

            for (FTPFile ftpFile : ftpFiles) {
                String name = ftpFile.getName();
                Map<String, Object> map = new HashMap<>();
                map.put("label", name);
                if (ftpFile.isFile()) {
                    if (set.contains(name.substring(0, name.indexOf(".")))) {
                        continue;
                    }

                    map.put("type", 0);
                    map.put("name", path + name);
                } else {
                    map.put("type", 1);
                    map.put("name", path + name + "/");
                }
                mapList.add(map);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw ExceptionAssertEnum.ServiceError.newException("查询子目录文件信息错误", e);
        }
        return mapList;
    }

    /***
     *
     * 获取当前目录的所有Ftp文件树
     * @param path 路径
     * @param ftpClient Ftp连接客户端
     * @return List<FtpTreeVo>
     */
    private List<FtpTreeVo> getLocalFtpFileTree(String path, FTPClient ftpClient) {
        List<FtpTreeVo> ftpTreeVoList = new ArrayList<>();

        //shp或者tif文件的名字
        Set<String> set = new HashSet<>();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(path);
            for (FTPFile ftpFile : ftpFiles) {
                String name = ftpFile.getName();
                boolean suffix = name.endsWith(".shp") || name.endsWith(".TIF");
                if (suffix && !set.contains(name.substring(0, name.indexOf(".")))) {
                    set.add(name.substring(0, name.indexOf(".")));
                    FtpTreeVo ftpTreeVo = new FtpTreeVo();
                    ftpTreeVo.setName(path + name);
                    ftpTreeVo.setType(0);
                    ftpTreeVo.setLabel(name);
                    ftpTreeVoList.add(ftpTreeVo);
                }
            }

            for (FTPFile ftpFile : ftpFiles) {
                String name = ftpFile.getName();
                FtpTreeVo ftpTreeVo = new FtpTreeVo();
                ftpTreeVo.setLabel(name);

                if (ftpFile.isFile()) {
                    if (set.contains(name.substring(0, name.indexOf(".")))) {
                        continue;
                    }
                    ftpTreeVo.setType(0);
                    ftpTreeVo.setName(path + name);

                } else {
                    ftpTreeVo.setType(1);
                    ftpTreeVo.setName(path + name + "/");
                    List<FtpTreeVo> localFtpFileTree = getLocalFtpFileTree(path + name + "/", ftpClient);
                    ftpTreeVo.setChildren(localFtpFileTree);
                }
                ftpTreeVoList.add(ftpTreeVo);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw ExceptionAssertEnum.ServiceError.newException("查询子目录文件信息错误", e);
        }
        return ftpTreeVoList;
    }

}