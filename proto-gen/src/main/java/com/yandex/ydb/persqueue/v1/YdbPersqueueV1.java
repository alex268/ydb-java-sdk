// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: kikimr/public/api/grpc/draft/ydb_persqueue_v1.proto

package tech.ydb.persqueue.v1;

public final class YdbPersqueueV1 {
  private YdbPersqueueV1() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n3kikimr/public/api/grpc/draft/ydb_persq" +
      "ueue_v1.proto\022\020Ydb.PersQueue.V1\032>kikimr/" +
      "public/api/protos/ydb_persqueue_cluster_" +
      "discovery.proto\032/kikimr/public/api/proto" +
      "s/ydb_persqueue_v1.proto2\325\002\n\020PersQueueSe" +
      "rvice\022r\n\016StreamingWrite\022-.Ydb.PersQueue." +
      "V1.StreamingWriteClientMessage\032-.Ydb.Per" +
      "sQueue.V1.StreamingWriteServerMessage(\0010" +
      "\001\022o\n\rStreamingRead\022,.Ydb.PersQueue.V1.St" +
      "reamingReadClientMessage\032,.Ydb.PersQueue",
      ".V1.StreamingReadServerMessage(\0010\001\022\\\n\023Ge" +
      "tReadSessionsInfo\022!.Ydb.PersQueue.V1.Rea" +
      "dInfoRequest\032\".Ydb.PersQueue.V1.ReadInfo" +
      "Response2\241\001\n\027ClusterDiscoveryService\022\205\001\n" +
      "\020DiscoverClusters\0227.Ydb.PersQueue.Cluste" +
      "rDiscovery.DiscoverClustersRequest\0328.Ydb" +
      ".PersQueue.ClusterDiscovery.DiscoverClus" +
      "tersResponseB \n\033tech.ydb.persqueue" +
      ".v1\370\001\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.getDescriptor(),
          tech.ydb.persqueue.YdbPersqueueV1.getDescriptor(),
        }, assigner);
    tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.getDescriptor();
    tech.ydb.persqueue.YdbPersqueueV1.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
