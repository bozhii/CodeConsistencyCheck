import cn.hutool.core.util.ObjectUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @program: CodeConsistencyCheck
 * @description:
 * @author: Mr.Zheng
 * @create: 2023-09-07 14:36
 **/
public class Main {

    private static ZipFile zip = null;
    private static byte[] buf = null;
    private static InputStream in = null;
    private static OutputStream out = null;

    private static List<String> recursiveSearch(String folderPath, Boolean jar) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        List<String> fileJarPathList = new ArrayList<>();
        List<String> fileAllPathList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    List<String> stringList = recursiveSearch(file.getAbsolutePath(), jar);
                    if (jar) {
                        fileJarPathList.addAll(stringList);
                    } else {
                        fileAllPathList.addAll(stringList);
                    }
                } else if (file.getName().toLowerCase().endsWith(".jar")) {
                    // 在这里执行你想要的操作
                    fileJarPathList.add(file.getPath());
                } else {
                    fileAllPathList.add(file.getPath());
                }
            }
        }
        if (jar) {
            return fileJarPathList;
        }
        return fileAllPathList;
    }

    private static String calculateMD5(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, md)) {
            // 读取文件内容，计算MD5值
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // 什么也不需要做
            }
        }

        // 获取计算得到的MD5值
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String calculateSHA256(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, md)) {
            // 读取文件内容，计算SHA-256值
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // 什么也不需要做
            }
        }

        // 获取计算得到的SHA-256值
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static boolean unZip(File zipFile, String descDir) {
        boolean flag = false;
        File pathFile = new File(descDir);
        if(!pathFile.exists()){
            pathFile.mkdirs();
        }
        try {
            //指定编码，否则压缩包里面不能有中文目录
            zip = new ZipFile(zipFile, StandardCharsets.UTF_8);
            for(Enumeration entries = zip.entries(); entries.hasMoreElements();){
                ZipEntry entry = (ZipEntry)entries.nextElement();
                String zipEntryName = entry.getName();
                in = zip.getInputStream(entry);
                String outPath = (descDir+zipEntryName).replace("/", File.separator);
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)));
                if(!file.exists()){
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if(new File(outPath).isDirectory()){
                    continue;
                }
                //保存文件路径信息
                out = new FileOutputStream(outPath);
                buf = new byte[2048];
                int len;
                while((len=in.read(buf))>0){
                    out.write(buf,0,len);
                }
                in.close();
                out.close();
            }
            flag = true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert in != null;
                in.close();
                assert out != null;
                out.close();
                //必须关闭，否则无法删除该zip文件
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public static Map<String, List<String>> getJarFilesHash(String path) throws IOException, NoSuchAlgorithmException {
        File file;
        file = new File(path);
        String fileNamePath = file.getAbsolutePath();
        fileNamePath = fileNamePath.replaceAll("\\.\\w+$", "");
        Boolean aBoolean = unZip(file, fileNamePath + "\\");
        if (!aBoolean) {
            System.out.println("解压失败");
        }
        while (true) {
            List<String> fileJarPathList = recursiveSearch(fileNamePath, true);
            if (fileJarPathList.size() == 0) {
                break;
            }
            for (String fileJarPath : fileJarPathList) {
                file = new File(fileJarPath);
                String absolutePath = file.getAbsolutePath();
                absolutePath = absolutePath.replaceAll("\\.\\w+$", "");
                aBoolean = unZip(file, absolutePath + "\\");

                if (!aBoolean) {
                    System.out.println("解压失败");
                } else {
                    file.delete();
                }
            }
        }
        List<String> fileAllPathList = recursiveSearch(fileNamePath, false);
        Map<String, List<String>> fileHash = new HashMap<>();
        for (String filePath :
                fileAllPathList) {
            List<String> hash = new ArrayList<>();
            hash.add(filePath);
            hash.add(calculateMD5(filePath));
            fileHash.put(calculateSHA256(filePath), hash);
        }
        return fileHash;

    }


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String srcFilePath = "";
        String dirFilePath = "";
        String srcFileName = "";
        String dirFileName = "";
        BufferedWriter writer;
        for (int i = 0; i < args.length; i++) {
            if ("-h".equals(args[i]) || "-help".equals(args[i])) {
                System.out.println("CodeConsistencyCheck");
                System.out.println("说明：程序功能为根据文件256哈希、MD5双重校验，比对出两个jar、zip压缩文件中差异文件。");
                System.out.println("参数说明：");
                System.out.println("-s: 原件文件");
                System.out.println("-d: 比对文件");
                return;
            }
            switch (args[i])
            {
                case "-s":
                    srcFilePath = args[i+1];
                    break;
                case "-d":
                    dirFilePath = args[i+1];
                    break;
                default:
                    break;
            }
        }
        if (ObjectUtil.isEmpty(srcFilePath) || ObjectUtil.isEmpty(dirFilePath)) {
            System.out.println("文件不存在");
            return;
        }
        if (!new File(srcFilePath).exists() || !new File(dirFilePath).exists()) {
            System.out.println("文件不存在");
            return;
        }

        System.out.println("CodeConsistencyCheck");
        System.out.println("说明：程序功能为根据文件256哈希、MD5双重校验，比对出两个jar、zip压缩文件中差异文件。");

        Map<String, List<String>> srcJarFilesHash = getJarFilesHash(srcFilePath);
        System.out.println(srcFilePath + ": 文件夹数" + srcJarFilesHash.size());
        Map<String, List<String>> dirJarFilesHash = getJarFilesHash(dirFilePath);
        System.out.println(dirFilePath + ": 文件夹数" + dirJarFilesHash.size());

        srcFileName = srcFilePath.replaceAll("\\.\\w+$", "");
        dirFileName = dirFilePath.replaceAll("\\.\\w+$", "");

        writer = new BufferedWriter(new FileWriter(srcFileName + ".txt"));
        for (Map.Entry<String, List<String>> entry: srcJarFilesHash.entrySet()) {
            String key = entry.getKey();
            List<String> list = dirJarFilesHash.get(key);
            if (list == null) {
                writer.write(key);
                writer.newLine();
                writer.write(entry.getValue().toString());
                writer.newLine();
                writer.newLine();
            } else {
                if (!list.get(1).equals(entry.getValue().get(1))) {
                    writer.write(key);
                    writer.newLine();
                    writer.write(entry.getValue().toString());
                    writer.newLine();
                    writer.newLine();
                }
            }
        }
        writer.close();

        System.out.println(new File(srcFileName).getName() + "、" + new File(dirFileName).getName() + "文件夹下所有文件哈希比对完成，文件哈希没有匹配成功的已输出到如下文件中。");
        System.out.println(srcFileName + ".txt");

        writer = new BufferedWriter(new FileWriter(dirFileName + ".txt"));
        for (Map.Entry<String, List<String>> entry: dirJarFilesHash.entrySet()) {
            String key = entry.getKey();
            List<String> list = srcJarFilesHash.get(key);
            if (list == null) {
                writer.write(key);
                writer.newLine();
                writer.write(entry.getValue().toString());
                writer.newLine();
                writer.newLine();
            } else {
                if (!list.get(1).equals(entry.getValue().get(1))) {
                    writer.write(key);
                    writer.newLine();
                    writer.write(entry.getValue().toString());
                    writer.newLine();
                    writer.newLine();
                }
            }
        }
        writer.close();
        System.out.println(new File(dirFileName).getName() + "、" + new File(srcFileName).getName()  + "文件夹下所有文件哈希比对完成，文件哈希没有匹配成功的已输出到如下文件中。");
        System.out.println(dirFileName + ".txt");
        System.out.println("文件结构说明: ");
        System.out.println("文件256哈希\n" +
                "[文件路径, 文件MD5]\n");
    }

}
