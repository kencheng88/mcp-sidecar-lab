# Kubernetes 部署文件說明 (K8s Configuration)

此目錄包含將 `biz` 與 `mcp-server-sidecar` 以 **Sidecar 模式** 部署至 Kubernetes 所需的 Manifests。

## 📄 檔案列表 (Files)

### 1. [deployment.yml](deployment.yml)
定義了單一 Pod 的 Deployment，其中包含兩個協同運作的容器：
- **biz**: 主要業務邏輯容器，曝露 Port `8080`。
- **mcp-server-sidecar**: MCP 協議伺服器，作為邊車運行，曝露 Port `8081`。
- **技術細節**:
    - 兩者透過 `localhost` 直接通訊，延遲極低。
    - 皆配置了 `LivenessProbe` 確保服務可用性。
    - `imagePullPolicy: Never` 確保在本地開發環境 (如 Minikube/Kind) 直接使用本地編譯的 Image。

### 2. [service.yml](service.yml)
定義了外部存取 `biz` 服務的管道。
- **功能**: 將外部 Port `80` 的請求導向 Pod 內 `biz` 容器的 `8080`。

## ⚙️ 部署命令 (Deployment Commands)

請確保您已完成映像檔編譯，然後執行：

```bash
# 修改 selector 並部署
kubectl apply -f deployment.yml
kubectl apply -f service.yml

# 查看 Pod 狀態 (應顯示 2/2 Containers Ready)
kubectl get pods
```
