FROM elasticsearch:8.13.0

# 将本地预下载的 IK 插件复制到镜像内安装（Docker 内部网络可访问）
COPY analysis-ik-8.13.0.zip /tmp/analysis-ik-8.13.0.zip
RUN elasticsearch-plugin install --batch file:///tmp/analysis-ik-8.13.0.zip