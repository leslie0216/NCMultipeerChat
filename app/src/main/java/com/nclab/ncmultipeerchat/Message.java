// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Messages.proto

package com.nclab.ncmultipeerchat;

public final class Message {
  private Message() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface PingMessageOrBuilder extends
      // @@protoc_insertion_point(interface_extends:PingMessage)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>optional uint32 token = 1;</code>
     */
    int getToken();

    /**
     * <code>optional .PingMessage.MsgType messageType = 2;</code>
     */
    int getMessageTypeValue();
    /**
     * <code>optional .PingMessage.MsgType messageType = 2;</code>
     */
    com.nclab.ncmultipeerchat.Message.PingMessage.MsgType getMessageType();

    /**
     * <code>optional double responseTime = 3;</code>
     */
    double getResponseTime();

    /**
     * <code>optional bool isReliable = 4;</code>
     */
    boolean getIsReliable();

    /**
     * <code>optional string message = 5;</code>
     */
    java.lang.String getMessage();
    /**
     * <code>optional string message = 5;</code>
     */
    com.google.protobuf.ByteString
        getMessageBytes();
  }
  /**
   * Protobuf type {@code PingMessage}
   */
  public  static final class PingMessage extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:PingMessage)
      PingMessageOrBuilder {
    // Use PingMessage.newBuilder() to construct.
    private PingMessage(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private PingMessage() {
      token_ = 0;
      messageType_ = 0;
      responseTime_ = 0D;
      isReliable_ = false;
      message_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private PingMessage(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) {
      this();
      int mutable_bitField0_ = 0;
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!input.skipField(tag)) {
                done = true;
              }
              break;
            }
            case 8: {

              token_ = input.readUInt32();
              break;
            }
            case 16: {
              int rawValue = input.readEnum();

              messageType_ = rawValue;
              break;
            }
            case 25: {

              responseTime_ = input.readDouble();
              break;
            }
            case 32: {

              isReliable_ = input.readBool();
              break;
            }
            case 42: {
              java.lang.String s = input.readStringRequireUtf8();

              message_ = s;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw new RuntimeException(e.setUnfinishedMessage(this));
      } catch (java.io.IOException e) {
        throw new RuntimeException(
            new com.google.protobuf.InvalidProtocolBufferException(
                e.getMessage()).setUnfinishedMessage(this));
      } finally {
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.nclab.ncmultipeerchat.Message.internal_static_PingMessage_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.nclab.ncmultipeerchat.Message.internal_static_PingMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.nclab.ncmultipeerchat.Message.PingMessage.class, com.nclab.ncmultipeerchat.Message.PingMessage.Builder.class);
    }

    /**
     * Protobuf enum {@code PingMessage.MsgType}
     */
    public enum MsgType
        implements com.google.protobuf.ProtocolMessageEnum {
      /**
       * <code>PING = 0;</code>
       */
      PING(0, 0),
      /**
       * <code>RESPONSE = 1;</code>
       */
      RESPONSE(1, 1),
      UNRECOGNIZED(-1, -1),
      ;

      /**
       * <code>PING = 0;</code>
       */
      public static final int PING_VALUE = 0;
      /**
       * <code>RESPONSE = 1;</code>
       */
      public static final int RESPONSE_VALUE = 1;


      public final int getNumber() {
        if (index == -1) {
          throw new java.lang.IllegalArgumentException(
              "Can't get the number of an unknown enum value.");
        }
        return value;
      }

      public static MsgType valueOf(int value) {
        switch (value) {
          case 0: return PING;
          case 1: return RESPONSE;
          default: return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<MsgType>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static final com.google.protobuf.Internal.EnumLiteMap<
          MsgType> internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<MsgType>() {
              public MsgType findValueByNumber(int number) {
                return MsgType.valueOf(number);
              }
            };

      public final com.google.protobuf.Descriptors.EnumValueDescriptor
          getValueDescriptor() {
        return getDescriptor().getValues().get(index);
      }
      public final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptorForType() {
        return getDescriptor();
      }
      public static final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptor() {
        return com.nclab.ncmultipeerchat.Message.PingMessage.getDescriptor().getEnumTypes().get(0);
      }

      private static final MsgType[] VALUES = values();

      public static MsgType valueOf(
          com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new java.lang.IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
          return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
      }

      private final int index;
      private final int value;

      private MsgType(int index, int value) {
        this.index = index;
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:PingMessage.MsgType)
    }

    public static final int TOKEN_FIELD_NUMBER = 1;
    private int token_;
    /**
     * <code>optional uint32 token = 1;</code>
     */
    public int getToken() {
      return token_;
    }

    public static final int MESSAGETYPE_FIELD_NUMBER = 2;
    private int messageType_;
    /**
     * <code>optional .PingMessage.MsgType messageType = 2;</code>
     */
    public int getMessageTypeValue() {
      return messageType_;
    }
    /**
     * <code>optional .PingMessage.MsgType messageType = 2;</code>
     */
    public com.nclab.ncmultipeerchat.Message.PingMessage.MsgType getMessageType() {
      com.nclab.ncmultipeerchat.Message.PingMessage.MsgType result = com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.valueOf(messageType_);
      return result == null ? com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.UNRECOGNIZED : result;
    }

    public static final int RESPONSETIME_FIELD_NUMBER = 3;
    private double responseTime_;
    /**
     * <code>optional double responseTime = 3;</code>
     */
    public double getResponseTime() {
      return responseTime_;
    }

    public static final int ISRELIABLE_FIELD_NUMBER = 4;
    private boolean isReliable_;
    /**
     * <code>optional bool isReliable = 4;</code>
     */
    public boolean getIsReliable() {
      return isReliable_;
    }

    public static final int MESSAGE_FIELD_NUMBER = 5;
    private volatile java.lang.Object message_;
    /**
     * <code>optional string message = 5;</code>
     */
    public java.lang.String getMessage() {
      java.lang.Object ref = message_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        message_ = s;
        return s;
      }
    }
    /**
     * <code>optional string message = 5;</code>
     */
    public com.google.protobuf.ByteString
        getMessageBytes() {
      java.lang.Object ref = message_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        message_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (token_ != 0) {
        output.writeUInt32(1, token_);
      }
      if (messageType_ != com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.PING.getNumber()) {
        output.writeEnum(2, messageType_);
      }
      if (responseTime_ != 0D) {
        output.writeDouble(3, responseTime_);
      }
      if (isReliable_ != false) {
        output.writeBool(4, isReliable_);
      }
      if (!getMessageBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessage.writeString(output, 5, message_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (token_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, token_);
      }
      if (messageType_ != com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.PING.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(2, messageType_);
      }
      if (responseTime_ != 0D) {
        size += com.google.protobuf.CodedOutputStream
          .computeDoubleSize(3, responseTime_);
      }
      if (isReliable_ != false) {
        size += com.google.protobuf.CodedOutputStream
          .computeBoolSize(4, isReliable_);
      }
      if (!getMessageBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessage.computeStringSize(5, message_);
      }
      memoizedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.nclab.ncmultipeerchat.Message.PingMessage parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(com.nclab.ncmultipeerchat.Message.PingMessage prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code PingMessage}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:PingMessage)
        com.nclab.ncmultipeerchat.Message.PingMessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.nclab.ncmultipeerchat.Message.internal_static_PingMessage_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.nclab.ncmultipeerchat.Message.internal_static_PingMessage_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.nclab.ncmultipeerchat.Message.PingMessage.class, com.nclab.ncmultipeerchat.Message.PingMessage.Builder.class);
      }

      // Construct using com.nclab.ncmultipeerchat.Message.PingMessage.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        token_ = 0;

        messageType_ = 0;

        responseTime_ = 0D;

        isReliable_ = false;

        message_ = "";

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.nclab.ncmultipeerchat.Message.internal_static_PingMessage_descriptor;
      }

      public com.nclab.ncmultipeerchat.Message.PingMessage getDefaultInstanceForType() {
        return com.nclab.ncmultipeerchat.Message.PingMessage.getDefaultInstance();
      }

      public com.nclab.ncmultipeerchat.Message.PingMessage build() {
        com.nclab.ncmultipeerchat.Message.PingMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.nclab.ncmultipeerchat.Message.PingMessage buildPartial() {
        com.nclab.ncmultipeerchat.Message.PingMessage result = new com.nclab.ncmultipeerchat.Message.PingMessage(this);
        result.token_ = token_;
        result.messageType_ = messageType_;
        result.responseTime_ = responseTime_;
        result.isReliable_ = isReliable_;
        result.message_ = message_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.nclab.ncmultipeerchat.Message.PingMessage) {
          return mergeFrom((com.nclab.ncmultipeerchat.Message.PingMessage)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.nclab.ncmultipeerchat.Message.PingMessage other) {
        if (other == com.nclab.ncmultipeerchat.Message.PingMessage.getDefaultInstance()) return this;
        if (other.getToken() != 0) {
          setToken(other.getToken());
        }
        if (other.messageType_ != 0) {
          setMessageTypeValue(other.getMessageTypeValue());
        }
        if (other.getResponseTime() != 0D) {
          setResponseTime(other.getResponseTime());
        }
        if (other.getIsReliable() != false) {
          setIsReliable(other.getIsReliable());
        }
        if (!other.getMessage().isEmpty()) {
          message_ = other.message_;
          onChanged();
        }
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.nclab.ncmultipeerchat.Message.PingMessage parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.nclab.ncmultipeerchat.Message.PingMessage) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int token_ ;
      /**
       * <code>optional uint32 token = 1;</code>
       */
      public int getToken() {
        return token_;
      }
      /**
       * <code>optional uint32 token = 1;</code>
       */
      public Builder setToken(int value) {
        
        token_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint32 token = 1;</code>
       */
      public Builder clearToken() {
        
        token_ = 0;
        onChanged();
        return this;
      }

      private int messageType_ = 0;
      /**
       * <code>optional .PingMessage.MsgType messageType = 2;</code>
       */
      public int getMessageTypeValue() {
        return messageType_;
      }
      /**
       * <code>optional .PingMessage.MsgType messageType = 2;</code>
       */
      public Builder setMessageTypeValue(int value) {
        messageType_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional .PingMessage.MsgType messageType = 2;</code>
       */
      public com.nclab.ncmultipeerchat.Message.PingMessage.MsgType getMessageType() {
        com.nclab.ncmultipeerchat.Message.PingMessage.MsgType result = com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.valueOf(messageType_);
        return result == null ? com.nclab.ncmultipeerchat.Message.PingMessage.MsgType.UNRECOGNIZED : result;
      }
      /**
       * <code>optional .PingMessage.MsgType messageType = 2;</code>
       */
      public Builder setMessageType(com.nclab.ncmultipeerchat.Message.PingMessage.MsgType value) {
        if (value == null) {
          throw new NullPointerException();
        }
        
        messageType_ = value.getNumber();
        onChanged();
        return this;
      }
      /**
       * <code>optional .PingMessage.MsgType messageType = 2;</code>
       */
      public Builder clearMessageType() {
        
        messageType_ = 0;
        onChanged();
        return this;
      }

      private double responseTime_ ;
      /**
       * <code>optional double responseTime = 3;</code>
       */
      public double getResponseTime() {
        return responseTime_;
      }
      /**
       * <code>optional double responseTime = 3;</code>
       */
      public Builder setResponseTime(double value) {
        
        responseTime_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional double responseTime = 3;</code>
       */
      public Builder clearResponseTime() {
        
        responseTime_ = 0D;
        onChanged();
        return this;
      }

      private boolean isReliable_ ;
      /**
       * <code>optional bool isReliable = 4;</code>
       */
      public boolean getIsReliable() {
        return isReliable_;
      }
      /**
       * <code>optional bool isReliable = 4;</code>
       */
      public Builder setIsReliable(boolean value) {
        
        isReliable_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional bool isReliable = 4;</code>
       */
      public Builder clearIsReliable() {
        
        isReliable_ = false;
        onChanged();
        return this;
      }

      private java.lang.Object message_ = "";
      /**
       * <code>optional string message = 5;</code>
       */
      public java.lang.String getMessage() {
        java.lang.Object ref = message_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          message_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>optional string message = 5;</code>
       */
      public com.google.protobuf.ByteString
          getMessageBytes() {
        java.lang.Object ref = message_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          message_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>optional string message = 5;</code>
       */
      public Builder setMessage(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        message_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional string message = 5;</code>
       */
      public Builder clearMessage() {
        
        message_ = getDefaultInstance().getMessage();
        onChanged();
        return this;
      }
      /**
       * <code>optional string message = 5;</code>
       */
      public Builder setMessageBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        message_ = value;
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }


      // @@protoc_insertion_point(builder_scope:PingMessage)
    }

    // @@protoc_insertion_point(class_scope:PingMessage)
    private static final com.nclab.ncmultipeerchat.Message.PingMessage DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new com.nclab.ncmultipeerchat.Message.PingMessage();
    }

    public static com.nclab.ncmultipeerchat.Message.PingMessage getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PingMessage>
        PARSER = new com.google.protobuf.AbstractParser<PingMessage>() {
      public PingMessage parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        try {
          return new PingMessage(input, extensionRegistry);
        } catch (RuntimeException e) {
          if (e.getCause() instanceof
              com.google.protobuf.InvalidProtocolBufferException) {
            throw (com.google.protobuf.InvalidProtocolBufferException)
                e.getCause();
          }
          throw e;
        }
      }
    };

    public static com.google.protobuf.Parser<PingMessage> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PingMessage> getParserForType() {
      return PARSER;
    }

    public com.nclab.ncmultipeerchat.Message.PingMessage getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_PingMessage_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_PingMessage_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\016Messages.proto\"\245\001\n\013PingMessage\022\r\n\005toke" +
      "n\030\001 \001(\r\022)\n\013messageType\030\002 \001(\0162\024.PingMessa" +
      "ge.MsgType\022\024\n\014responseTime\030\003 \001(\001\022\022\n\nisRe" +
      "liable\030\004 \001(\010\022\017\n\007message\030\005 \001(\t\"!\n\007MsgType" +
      "\022\010\n\004PING\020\000\022\014\n\010RESPONSE\020\001B$\n\031com.nclab.nc" +
      "multipeerchatB\007Messageb\006proto3"
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
        }, assigner);
    internal_static_PingMessage_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_PingMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_PingMessage_descriptor,
        new java.lang.String[] { "Token", "MessageType", "ResponseTime", "IsReliable", "Message", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
