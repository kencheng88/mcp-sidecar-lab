# Implementation Plan - MCP Sidecar Lab Deployment

This plan outlines the steps to containerize the `biz` microservice and the `mcp-server-sidecar` MCP server, and deploy them together in a single Kubernetes Pod.

## Proposed Changes

### Configuration
#### [MODIFY] [application.properties](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/mcp-server-sidecar/src/main/resources/application.properties)
- Change `server.port` to `8081` to avoid conflict with the `biz` service (defaulting to `8080`) when running in the same Pod.

---

### [biz](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/biz)
#### [NEW] [Dockerfile](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/biz/Dockerfile)
- Multi-stage Dockerfile to build and package the Maven project.
#### [NEW] [README.md](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/biz/README.md)
- Description of the business microservice.

---

### [mcp-server-sidecar](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/mcp-server-sidecar)
#### [NEW] [Dockerfile](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/mcp-server-sidecar/Dockerfile)
- Multi-stage Dockerfile to build and package the Maven project.
#### [NEW] [README.md](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/mcp-server-sidecar/README.md)
- Description of the MCP sidecar server.

---

### [Kubernetes Manifests](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/k8s)
#### [NEW] [deployment.yml](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/k8s/deployment.yml)
- Kubernetes Deployment featuring a single Pod with two containers: `biz` and `mcp-server-sidecar`.
#### [NEW] [service.yml](file:///Users/kencheng/Documents/lab/mcp-sidecar-lab/k8s/service.yml)
- Kubernetes Service to expose the `biz` microservice.

## Verification Plan

### Automated Tests
- Validate Kubernetes YAML files using `kubectl diff` (if cluster available) or just syntax check.
- Verify Dockerfiles can be built (dry run with `docker build --no-cache`).

### Manual Verification
- Review the generated `Dockerfile` and `deployment.yml` to ensure correctly configured container ports and sidecar architecture.
- Check that `mcp-server-sidecar` is configured to listen on port 8081.
