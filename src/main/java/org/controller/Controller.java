package org.controller;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.ArrayList;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.Model.PodInfo;
import org.Model.NamespaceInfo;
@Path("/pods")
public class Controller {

    // Thiết lập cấu hình kết nối
    Config config = new ConfigBuilder()
            .withMasterUrl("https://127.0.0.1:57048")
            .withTrustCerts(true)
            .build();

    @GET
    @Path("/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPods(@PathParam("namespace") String namespace) {

        // Tạo client Kubernetes
        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            // Lấy danh sách các pods trong namespace
            PodList podList = client.pods().inNamespace(namespace).list();

            // Lấy ID, tên, địa chỉ IP và trạng thái của các Pod
            List<PodInfo> podInfoList = new ArrayList<>();
            for (Pod pod : podList.getItems()) {
                String podId = pod.getMetadata().getUid();
                String podName = pod.getMetadata().getName();
                String podIP = pod.getStatus().getPodIP();
                String podStatus = pod.getStatus().getPhase();
                podInfoList.add(new PodInfo(podId, podName, podIP, podStatus));
            }

            // Convert danh sách thông tin Pod thành chuỗi JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(podInfoList);

            // Trả về danh sách thông tin Pod dưới dạng JSON
            return Response.ok(json).build();
        } catch (JsonProcessingException e) {
            // Xử lý lỗi nếu có
            return Response.serverError().build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNamespaces() {


        // Tạo client Kubernetes
        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            // Lấy danh sách các namespaces
            NamespaceList namespaceList = client.namespaces().list();

            // Tạo danh sách NamespaceInfo
            List<NamespaceInfo> namespaceInfoList = new ArrayList<>();
            for (Namespace namespace : namespaceList.getItems()) {
                String namespaceId = namespace.getMetadata().getUid();
                String namespaceName = namespace.getMetadata().getName();
                namespaceInfoList.add(new NamespaceInfo(namespaceId, namespaceName));
            }

            // Convert danh sách NamespaceInfo thành chuỗi JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(namespaceInfoList);

            // Trả về danh sách namespaces dưới dạng JSON
            return Response.ok(json).build();
        } catch (JsonProcessingException e) {
            // Xử lý lỗi nếu có
            return Response.serverError().build();
        }
    }

    @DELETE
    public void Healing(){

        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            // Tắt các pod có nhãn app: "reddit" trong namespace "default"
            client.pods().inNamespace("default").withLabel("app", "mongo").delete();
            client.pods().inNamespace("default").withLabel("app", "webapp").delete();
            System.out.println("Các pod đã được tắt.");
        }
    }

    @DELETE
    @Path("/terminate")
    public void Terminate(){


        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            client.apps().deployments().inNamespace("default").withName("mongo-deployment").delete();
            client.apps().deployments().inNamespace("default").withName("webapp-deployment").delete();
            client.services().inNamespace("default").withName("mongo-service").delete();
            client.services().inNamespace("default").withName("webapp-service").delete();
            System.out.println("Pod đã được tắt.");
        }
    }

    @POST
    @Path("/instantiate")
    public void Instantiate(){

        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            // Load Deployment YAML Manifest into Java object
            Deployment deploy1 = client.apps().deployments()
                    .load(Controller.class.getResourceAsStream("/mongo-app.yaml"))
                    .item();
            // Apply it to Kubernetes Cluster
            client.apps().deployments().inNamespace("default").resource(deploy1).create();

            Service svc1 = client.services()
                    .load(Controller.class.getResourceAsStream("/mongo-app-service.yaml"))
                    .item();

            client.services().inNamespace("default").resource(svc1).create();
            // Load Deployment YAML Manifest into Java object
            Deployment deploy2 = client.apps().deployments()
                    .load(Controller.class.getResourceAsStream("/web-app.yaml"))
                    .item();
            // Apply it to Kubernetes Cluster
            client.apps().deployments().inNamespace("default").resource(deploy2).create();


            Service svc2 = client.services()
                    .load(Controller.class.getResourceAsStream("/web-app-service.yaml"))
                    .item();

            client.services().inNamespace("default").resource(svc2).create();
            System.out.println("Đã bật VNF thành công");
        }
             catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }

    }

    @PUT
    @Path("/scale/{count}")
    public void Scale(@PathParam("count") int count){

        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            // Lấy thông tin deployment
            Deployment deployment = client.apps().deployments().inNamespace("default").withName("reddit-deployment").get();

            // Thiết lập số lượng replicas
            deployment.getSpec().setReplicas(count);

            // Cập nhật deployment
            client.apps().deployments().inNamespace("default").createOrReplace(deployment);

            System.out.println("Scale thành công");
        }
    }
}