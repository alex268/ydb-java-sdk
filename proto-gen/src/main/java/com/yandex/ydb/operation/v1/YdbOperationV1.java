// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: kikimr/public/api/grpc/ydb_operation_v1.proto

package tech.ydb.operation.v1;

public final class YdbOperationV1 {
  private YdbOperationV1() {}
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
      "\n-kikimr/public/api/grpc/ydb_operation_v" +
      "1.proto\022\020Ydb.Operation.V1\032\033google/protob" +
      "uf/empty.proto\032,kikimr/public/api/protos" +
      "/ydb_operation.proto2\205\003\n\020OperationServic" +
      "e\022Y\n\014GetOperation\022#.Ydb.Operations.GetOp" +
      "erationRequest\032$.Ydb.Operations.GetOpera" +
      "tionResponse\022Q\n\017CancelOperation\022&.Ydb.Op" +
      "erations.CancelOperationRequest\032\026.google" +
      ".protobuf.Empty\022b\n\017ForgetOperation\022&.Ydb" +
      ".Operations.ForgetOperationRequest\032\'.Ydb",
      ".Operations.ForgetOperationResponse\022_\n\016L" +
      "istOperations\022%.Ydb.Operations.ListOpera" +
      "tionsRequest\032&.Ydb.Operations.ListOperat" +
      "ionsResponseB\035\n\033tech.ydb.operation" +
      ".v1b\006proto3"
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
          com.google.protobuf.EmptyProto.getDescriptor(),
          tech.ydb.OperationProtos.getDescriptor(),
        }, assigner);
    com.google.protobuf.EmptyProto.getDescriptor();
    tech.ydb.OperationProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
