/*
 * Copyright 2019 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.acinq.eclair.wire

import scodec.codecs._
import fr.acinq.eclair._
import fr.acinq.eclair.wire.CommonCodecs._
import fr.acinq.eclair.wire.ChannelCodecs.channelVersionCodec
import scodec.bits.ByteVector
import scodec.Codec

/**
 * Created by PM on 15/11/2016.
 */
object LightningMessageCodecs {

  val featuresCodec: Codec[Features] = varsizebinarydata.xmap[Features](
    { bytes => Features(bytes) },
    { features => features.toByteVector }
  )

  /** For historical reasons, features are divided into two feature bitmasks. We only send from the second one, but we allow receiving in both. */
  val combinedFeaturesCodec: Codec[Features] = (
    ("globalFeatures" | varsizebinarydata) ::
      ("localFeatures" | varsizebinarydata)).as[(ByteVector, ByteVector)].xmap[Features](
    { case (gf, lf) =>
      val length = gf.length.max(lf.length)
      Features(gf.padLeft(length) | lf.padLeft(length))
    },
    { features => (ByteVector.empty, features.toByteVector) })

  val initCodec: Codec[Init] = (("features" | combinedFeaturesCodec) :: ("tlvStream" | InitTlvCodecs.initTlvCodec)).as[Init]

  val errorCodec: Codec[Error] = (
    ("channelId" | bytes32) ::
      ("data" | varsizebinarydata)).as[Error]

  val pingCodec: Codec[Ping] = (
    ("pongLength" | uint16) ::
      ("data" | varsizebinarydata)).as[Ping]

  val pongCodec: Codec[Pong] =
    ("data" | varsizebinarydata).as[Pong]

  val channelReestablishCodec: Codec[ChannelReestablish] = (
    ("channelId" | bytes32) ::
      ("nextLocalCommitmentNumber" | uint64overflow) ::
      ("nextRemoteRevocationNumber" | uint64overflow) ::
      ("yourLastPerCommitmentSecret" | privateKey) ::
      ("myCurrentPerCommitmentPoint" | publicKey)).as[ChannelReestablish]

  val openChannelCodec: Codec[OpenChannel] = (
    ("chainHash" | bytes32) ::
      ("temporaryChannelId" | bytes32) ::
      ("fundingSatoshis" | satoshi) ::
      ("pushMsat" | millisatoshi) ::
      ("dustLimitSatoshis" | satoshi) ::
      ("maxHtlcValueInFlightMsat" | uint64) ::
      ("channelReserveSatoshis" | satoshi) ::
      ("htlcMinimumMsat" | millisatoshi) ::
      ("feeratePerKw" | feeratePerKw) ::
      ("toSelfDelay" | cltvExpiryDelta) ::
      ("maxAcceptedHtlcs" | uint16) ::
      ("fundingPubkey" | publicKey) ::
      ("revocationBasepoint" | publicKey) ::
      ("paymentBasepoint" | publicKey) ::
      ("delayedPaymentBasepoint" | publicKey) ::
      ("htlcBasepoint" | publicKey) ::
      ("firstPerCommitmentPoint" | publicKey) ::
      ("channelFlags" | byte) ::
      ("tlvStream" | OpenChannelTlv.openTlvCodec)).as[OpenChannel]

  val acceptChannelCodec: Codec[AcceptChannel] = (
    ("temporaryChannelId" | bytes32) ::
      ("dustLimitSatoshis" | satoshi) ::
      ("maxHtlcValueInFlightMsat" | uint64) ::
      ("channelReserveSatoshis" | satoshi) ::
      ("htlcMinimumMsat" | millisatoshi) ::
      ("minimumDepth" | uint32) ::
      ("toSelfDelay" | cltvExpiryDelta) ::
      ("maxAcceptedHtlcs" | uint16) ::
      ("fundingPubkey" | publicKey) ::
      ("revocationBasepoint" | publicKey) ::
      ("paymentBasepoint" | publicKey) ::
      ("delayedPaymentBasepoint" | publicKey) ::
      ("htlcBasepoint" | publicKey) ::
      ("firstPerCommitmentPoint" | publicKey) ::
      ("tlvStream" | AcceptChannelTlv.acceptTlvCodec)).as[AcceptChannel]

  val fundingCreatedCodec: Codec[FundingCreated] = (
    ("temporaryChannelId" | bytes32) ::
      ("fundingTxid" | bytes32) ::
      ("fundingOutputIndex" | uint16) ::
      ("signature" | bytes64)).as[FundingCreated]

  val fundingSignedCodec: Codec[FundingSigned] = (
    ("channelId" | bytes32) ::
      ("signature" | bytes64)).as[FundingSigned]

  val fundingLockedCodec: Codec[FundingLocked] = (
    ("channelId" | bytes32) ::
      ("nextPerCommitmentPoint" | publicKey)).as[FundingLocked]

  val shutdownCodec: Codec[wire.Shutdown] = (
    ("channelId" | bytes32) ::
      ("scriptPubKey" | varsizebinarydata)).as[Shutdown]

  val closingSignedCodec: Codec[ClosingSigned] = (
    ("channelId" | bytes32) ::
      ("feeSatoshis" | satoshi) ::
      ("signature" | bytes64)).as[ClosingSigned]

  val updateAddHtlcCodec: Codec[UpdateAddHtlc] = (
    ("channelId" | bytes32) ::
      ("id" | uint64overflow) ::
      ("amountMsat" | millisatoshi) ::
      ("paymentHash" | bytes32) ::
      ("expiry" | cltvExpiry) ::
      ("onionRoutingPacket" | OnionCodecs.paymentOnionPacketCodec) ::
      ("partId" | varsizebinarydata)).as[UpdateAddHtlc]

  val updateFulfillHtlcCodec: Codec[UpdateFulfillHtlc] = (
    ("channelId" | bytes32) ::
      ("id" | uint64overflow) ::
      ("paymentPreimage" | bytes32)).as[UpdateFulfillHtlc]

  val updateFailHtlcCodec: Codec[UpdateFailHtlc] = (
    ("channelId" | bytes32) ::
      ("id" | uint64overflow) ::
      ("reason" | varsizebinarydata)).as[UpdateFailHtlc]

  val updateFailMalformedHtlcCodec: Codec[UpdateFailMalformedHtlc] = (
    ("channelId" | bytes32) ::
      ("id" | uint64overflow) ::
      ("onionHash" | bytes32) ::
      ("failureCode" | uint16)).as[UpdateFailMalformedHtlc]

  val commitSigCodec: Codec[CommitSig] = (
    ("channelId" | bytes32) ::
      ("signature" | bytes64) ::
      ("htlcSignatures" | listofsignatures)).as[CommitSig]

  val revokeAndAckCodec: Codec[RevokeAndAck] = (
    ("channelId" | bytes32) ::
      ("perCommitmentSecret" | privateKey) ::
      ("nextPerCommitmentPoint" | publicKey)
    ).as[RevokeAndAck]

  val updateFeeCodec: Codec[UpdateFee] = (
    ("channelId" | bytes32) ::
      ("feeratePerKw" | feeratePerKw)).as[UpdateFee]

  val announcementSignaturesCodec: Codec[AnnouncementSignatures] = (
    ("channelId" | bytes32) ::
      ("shortChannelId" | shortchannelid) ::
      ("nodeSignature" | bytes64) ::
      ("bitcoinSignature" | bytes64)).as[AnnouncementSignatures]

  val channelAnnouncementWitnessCodec =
    ("features" | featuresCodec) ::
      ("chainHash" | bytes32) ::
      ("shortChannelId" | shortchannelid) ::
      ("nodeId1" | publicKey) ::
      ("nodeId2" | publicKey) ::
      ("bitcoinKey1" | publicKey) ::
      ("bitcoinKey2" | publicKey) ::
      ("unknownFields" | bytes)

  val channelAnnouncementCodec: Codec[ChannelAnnouncement] = (
    ("nodeSignature1" | bytes64) ::
      ("nodeSignature2" | bytes64) ::
      ("bitcoinSignature1" | bytes64) ::
      ("bitcoinSignature2" | bytes64) ::
      channelAnnouncementWitnessCodec).as[ChannelAnnouncement]

  val nodeAnnouncementWitnessCodec =
    ("features" | featuresCodec) ::
      ("timestamp" | uint32) ::
      ("nodeId" | publicKey) ::
      ("rgbColor" | rgb) ::
      ("alias" | zeropaddedstring(32)) ::
      ("addresses" | listofnodeaddresses) ::
      ("unknownFields" | bytes)

  val nodeAnnouncementCodec: Codec[NodeAnnouncement] = (
    ("signature" | bytes64) ::
      nodeAnnouncementWitnessCodec).as[NodeAnnouncement]

  val channelUpdateChecksumCodec =
    ("chainHash" | bytes32) ::
      ("shortChannelId" | shortchannelid) ::
      (("messageFlags" | byte) >>:~ { messageFlags =>
        ("channelFlags" | byte) ::
          ("cltvExpiryDelta" | cltvExpiryDelta) ::
          ("htlcMinimumMsat" | millisatoshi) ::
          ("feeBaseMsat" | millisatoshi32) ::
          ("feeProportionalMillionths" | uint32) ::
          ("htlcMaximumMsat" | conditional((messageFlags & 1) != 0, millisatoshi))
      })

  val channelUpdateWitnessCodec =
    ("chainHash" | bytes32) ::
      ("shortChannelId" | shortchannelid) ::
      ("timestamp" | uint32) ::
      (("messageFlags" | byte) >>:~ { messageFlags =>
        ("channelFlags" | byte) ::
          ("cltvExpiryDelta" | cltvExpiryDelta) ::
          ("htlcMinimumMsat" | millisatoshi) ::
          ("feeBaseMsat" | millisatoshi32) ::
          ("feeProportionalMillionths" | uint32) ::
          ("htlcMaximumMsat" | conditional((messageFlags & 1) != 0, millisatoshi)) ::
          ("unknownFields" | bytes)
      })

  val channelUpdateCodec: Codec[ChannelUpdate] = (
    ("signature" | bytes64) ::
      channelUpdateWitnessCodec).as[ChannelUpdate]

  val encodedShortChannelIdsCodec: Codec[EncodedShortChannelIds] =
    discriminated[EncodedShortChannelIds].by(byte)
      .\(0) {
        case a@EncodedShortChannelIds(_, Nil) => a // empty list is always encoded with encoding type 'uncompressed' for compatibility with other implementations
        case a@EncodedShortChannelIds(EncodingType.UNCOMPRESSED, _) => a
      }((provide[EncodingType](EncodingType.UNCOMPRESSED) :: list(shortchannelid)).as[EncodedShortChannelIds])
      .\(1) {
        case a@EncodedShortChannelIds(EncodingType.COMPRESSED_ZLIB, _) => a
      }((provide[EncodingType](EncodingType.COMPRESSED_ZLIB) :: zlib(list(shortchannelid))).as[EncodedShortChannelIds])


  val queryShortChannelIdsCodec: Codec[QueryShortChannelIds] = {
    Codec(
      ("chainHash" | bytes32) ::
        ("shortChannelIds" | variableSizeBytes(uint16, encodedShortChannelIdsCodec)) ::
        ("tlvStream" | QueryShortChannelIdsTlv.codec)
    ).as[QueryShortChannelIds]
  }

  val replyShortChanelIdsEndCodec: Codec[ReplyShortChannelIdsEnd] = (
    ("chainHash" | bytes32) ::
      ("complete" | byte)
    ).as[ReplyShortChannelIdsEnd]

  val queryChannelRangeCodec: Codec[QueryChannelRange] = {
    Codec(
      ("chainHash" | bytes32) ::
        ("firstBlockNum" | uint32) ::
        ("numberOfBlocks" | uint32) ::
        ("tlvStream" | QueryChannelRangeTlv.codec)
    ).as[QueryChannelRange]
  }

  val replyChannelRangeCodec: Codec[ReplyChannelRange] = {
    Codec(
      ("chainHash" | bytes32) ::
        ("firstBlockNum" | uint32) ::
        ("numberOfBlocks" | uint32) ::
        ("syncComplete" | byte) ::
        ("shortChannelIds" | variableSizeBytes(uint16, encodedShortChannelIdsCodec)) ::
        ("tlvStream" | ReplyChannelRangeTlv.codec)
    ).as[ReplyChannelRange]
  }

  val gossipTimestampFilterCodec: Codec[GossipTimestampFilter] = (
    ("chainHash" | bytes32) ::
      ("firstTimestamp" | uint32) ::
      ("timestampRange" | uint32)
    ).as[GossipTimestampFilter]

  val unknownMessageCodec: Codec[UnknownMessage] = (
    ("tag" | uint16) ::
      ("message" | bits)
    ).as[UnknownMessage]

  //

  val lightningMessageCodec: DiscriminatorCodec[LightningMessage, Int] =
    discriminated[LightningMessage].by(uint16)
      .typecase(16, initCodec)
      .typecase(17, errorCodec)
      .typecase(18, pingCodec)
      .typecase(19, pongCodec)
      .typecase(32, openChannelCodec)
      .typecase(33, acceptChannelCodec)
      .typecase(34, fundingCreatedCodec)
      .typecase(35, fundingSignedCodec)
      .typecase(36, fundingLockedCodec)
      .typecase(38, shutdownCodec)
      .typecase(39, closingSignedCodec)
      .typecase(128, updateAddHtlcCodec)
      .typecase(130, updateFulfillHtlcCodec)
      .typecase(131, updateFailHtlcCodec)
      .typecase(132, commitSigCodec)
      .typecase(133, revokeAndAckCodec)
      .typecase(134, updateFeeCodec)
      .typecase(135, updateFailMalformedHtlcCodec)
      .typecase(136, channelReestablishCodec)
      .typecase(256, channelAnnouncementCodec)
      .typecase(257, nodeAnnouncementCodec)
      .typecase(258, channelUpdateCodec)
      .typecase(259, announcementSignaturesCodec)
      .typecase(261, queryShortChannelIdsCodec)
      .typecase(262, replyShortChanelIdsEndCodec)
      .typecase(263, queryChannelRangeCodec)
      .typecase(264, replyChannelRangeCodec)
      .typecase(265, gossipTimestampFilterCodec)

  //

  val lightningMessageCodecWithFallback: Codec[LightningMessage] =
    discriminatorWithDefault(lightningMessageCodec, unknownMessageCodec.upcast)
}

object HostedMessagesCodecs {
  val invokeHostedChannelCodec = {
    (bytes32 withContext "chainHash") ::
      (varsizebinarydata withContext "refundScriptPubKey") ::
      (varsizebinarydata withContext "secret")
  }.as[InvokeHostedChannel]

  val initHostedChannelCodec = {
    (uint64 withContext "maxHtlcValueInFlightMsat") ::
      (millisatoshi withContext "htlcMinimumMsat") ::
      (uint16 withContext "maxAcceptedHtlcs") ::
      (millisatoshi withContext "channelCapacityMsat") ::
      (uint16 withContext "liabilityDeadlineBlockdays") ::
      (satoshi withContext "minimalOnchainRefundAmountSatoshis") ::
      (millisatoshi withContext "initialClientBalanceMsat") ::
      (channelVersionCodec withContext "version")
  }.as[InitHostedChannel]

  val hostedChannelBrandingCodec = {
    (rgb withContext "rgbColor") ::
      (varsizebinarydata withContext "pngIcon") ::
      (variableSizeBytes(uint16, utf8) withContext "contactInfo")
  }.as[HostedChannelBranding]

  val lastCrossSignedStateCodec: Codec[LastCrossSignedState] = {
    (bool withContext "isHost") ::
      (varsizebinarydata withContext "refundScriptPubKey") ::
      (initHostedChannelCodec withContext "initHostedChannel") ::
      (uint32 withContext "blockDay") ::
      (millisatoshi withContext "localBalanceMsat") ::
      (millisatoshi withContext "remoteBalanceMsat") ::
      (uint32 withContext "localUpdates") ::
      (uint32 withContext "remoteUpdates") ::
      (listOfN(uint16, LightningMessageCodecs.updateAddHtlcCodec) withContext "incomingHtlcs") ::
      (listOfN(uint16, LightningMessageCodecs.updateAddHtlcCodec) withContext "outgoingHtlcs") ::
      (bytes64 withContext "remoteSigOfLocal") ::
      (bytes64 withContext "localSigOfRemote")
  }.as[LastCrossSignedState]

  val stateUpdateCodec = {
    (uint32 withContext "blockDay") ::
      (uint32 withContext "localUpdates") ::
      (uint32 withContext "remoteUpdates") ::
      (bytes64 withContext "localSigOfRemoteLCSS")
  }.as[StateUpdate]

  val stateOverrideCodec = {
    (uint32 withContext "blockDay") ::
      (millisatoshi withContext "localBalanceMsat") ::
      (uint32 withContext "localUpdates") ::
      (uint32 withContext "remoteUpdates") ::
      (bytes64 withContext "localSigOfRemoteLCSS")
  }.as[StateOverride]

  val refundPendingCodec = (uint32 withContext "startedAt").as[RefundPending]

  val announcementSignatureCodec = {
    (bytes64 withContext "nodeSignature") ::
      (bool withContext "wantsReply")
  }.as[AnnouncementSignature]

  val resizeChannelCodec = {
    (satoshi withContext "newCapacity") ::
      (bytes64 withContext "clientSig")
  }.as[ResizeChannel]

  val queryPublicHostedChannelsCodec = (bytes32 withContext "chainHash").as[QueryPublicHostedChannels]

  val replyPublicHostedChannelsEndCodec = (bytes32 withContext "chainHash").as[ReplyPublicHostedChannelsEnd]

  final val HC_INVOKE_HOSTED_CHANNEL_TAG = 65535
  final val HC_INIT_HOSTED_CHANNEL_TAG = 65533
  final val HC_LAST_CROSS_SIGNED_STATE_TAG = 65531
  final val HC_STATE_UPDATE_TAG = 65529
  final val HC_STATE_OVERRIDE_TAG = 65527
  final val HC_HOSTED_CHANNEL_BRANDING_TAG = 65525
  final val HC_REFUND_PENDING_TAG = 65523
  final val HC_ANNOUNCEMENT_SIGNATURE_TAG = 65521
  final val HC_RESIZE_CHANNEL_TAG = 65519
  final val HC_QUERY_PUBLIC_HOSTED_CHANNELS_TAG = 65517
  final val HC_REPLY_PUBLIC_HOSTED_CHANNELS_END_TAG = 65515

  final val PHC_ANNOUNCE_GOSSIP_TAG = 65513
  final val PHC_ANNOUNCE_SYNC_TAG = 65511
  final val PHC_UPDATE_GOSSIP_TAG = 65509
  final val PHC_UPDATE_SYNC_TAG = 65507

  final val HC_UPDATE_ADD_HTLC_TAG = 65505
  final val HC_UPDATE_FULFILL_HTLC_TAG = 65503
  final val HC_UPDATE_FAIL_HTLC_TAG = 65501
  final val HC_UPDATE_FAIL_MALFORMED_HTLC_TAG = 65499
  final val HC_ERROR_TAG = 65497
}

object SwapCodecs {
  private val text = variableSizeBytes(uint16, utf8)
  private val optionalText = optional(bool, text)

  val swapInResponseCodec = {
    ("btcAddress" | text) ::
      ("minChainDeposit" | satoshi)
  }.as[SwapInResponse]

  val swapInPaymentRequestCodec = {
    ("paymentRequest" | text) ::
      ("id" | uint32)
  }.as[SwapInPaymentRequest]

  val swapInPaymentDeniedCodec = {
    ("id" | uint32) ::
      ("reason" | uint32)
  }.as[SwapInPaymentDenied]

  val pendingDepositCodec = {
    ("id" | uint32) ::
      ("lnPaymentId" | optionalText) ::
      ("lnStatus" | uint32) ::
      ("btcAddress" | text) ::
      ("outIndex" | uint32) ::
      ("txid" | text) ::
      ("amountSat" | uint32) ::
      ("depth" | uint32) ::
      ("stamp" | uint32)
  }.as[ChainDeposit]

  val swapInStateCodec = {
    ("pending" | listOfN(uint16, pendingDepositCodec)) ::
      ("ready" | listOfN(uint16, pendingDepositCodec)) ::
      ("processing" | listOfN(uint16, pendingDepositCodec))
  }.as[SwapInState]

  // SwapOut

  val blockTargetAndFeeCodec = {
    ("blockTarget" | uint16) ::
      ("fee" | satoshi)
  }.as[BlockTargetAndFee]

  val keyedBlockTargetAndFeeCodec = {
    ("feerates" | listOfN(uint16, blockTargetAndFeeCodec)) ::
      ("feerateKey" | bytes32)
  }.as[KeyedBlockTargetAndFee]

  val swapOutFeeratesCodec = {
    ("feerates" | keyedBlockTargetAndFeeCodec) ::
      ("providerCanHandle" | satoshi) ::
      ("minWithdrawable" | satoshi)
  }.as[SwapOutFeerates]

  val swapOutTransactionRequestCodec = {
    ("amount" | satoshi) ::
      ("btcAddress" | text) ::
      ("blockTarget" | uint16) ::
      ("feerateKey" | bytes32)
  }.as[SwapOutTransactionRequest]

  val swapOutTransactionResponseCodec = {
    ("paymentRequest" | text) ::
      ("amount" | satoshi) ::
      ("btcAddress" | text) ::
      ("fee" | satoshi)
  }.as[SwapOutTransactionResponse]

  val swapOutTransactionDeniedCodec = {
    ("btcAddress" | text) ::
      ("reason" | uint32)
  }.as[SwapOutTransactionDenied]

  final val SWAP_IN_REQUEST_MESSAGE_TAG = 55037
  final val SWAP_IN_RESPONSE_MESSAGE_TAG = 55035
  final val SWAP_IN_PAYMENT_REQUEST_MESSAGE_TAG = 55033
  final val SWAP_IN_PAYMENT_DENIED_MESSAGE_TAG = 55031
  final val SWAP_IN_STATE_MESSAGE_TAG = 55029

  final val SWAP_OUT_REQUEST_MESSAGE_TAG = 55027
  final val SWAP_OUT_FEERATES_MESSAGE_TAG = 55025
  final val SWAP_OUT_TRANSACTION_REQUEST_MESSAGE_TAG = 55023
  final val SWAP_OUT_TRANSACTION_RESPONSE_MESSAGE_TAG = 55021
  final val SWAP_OUT_TRANSACTION_DENIED_MESSAGE_TAG = 55019
}

object ExtMessageMapping {
  import HostedMessagesCodecs._
  import SwapCodecs._

  def decode(msg: UnknownMessage): LightningMessage = msg.tag match {
    case HC_INVOKE_HOSTED_CHANNEL_TAG => invokeHostedChannelCodec.decode(msg.data).require.value
    case HC_INIT_HOSTED_CHANNEL_TAG => initHostedChannelCodec.decode(msg.data).require.value
    case HC_LAST_CROSS_SIGNED_STATE_TAG => lastCrossSignedStateCodec.decode(msg.data).require.value
    case HC_STATE_UPDATE_TAG => stateUpdateCodec.decode(msg.data).require.value
    case HC_STATE_OVERRIDE_TAG => stateOverrideCodec.decode(msg.data).require.value
    case HC_HOSTED_CHANNEL_BRANDING_TAG => hostedChannelBrandingCodec.decode(msg.data).require.value
    case HC_REFUND_PENDING_TAG => refundPendingCodec.decode(msg.data).require.value
    case HC_RESIZE_CHANNEL_TAG => resizeChannelCodec.decode(msg.data).require.value
    case HC_QUERY_PUBLIC_HOSTED_CHANNELS_TAG => queryPublicHostedChannelsCodec.decode(msg.data).require.value
    case HC_REPLY_PUBLIC_HOSTED_CHANNELS_END_TAG => replyPublicHostedChannelsEndCodec.decode(msg.data).require.value
    case PHC_ANNOUNCE_GOSSIP_TAG => LightningMessageCodecs.channelAnnouncementCodec.decode(msg.data).require.value
    case PHC_ANNOUNCE_SYNC_TAG => LightningMessageCodecs.channelAnnouncementCodec.decode(msg.data).require.value
    case PHC_UPDATE_GOSSIP_TAG => LightningMessageCodecs.channelUpdateCodec.decode(msg.data).require.value
    case PHC_UPDATE_SYNC_TAG => LightningMessageCodecs.channelUpdateCodec.decode(msg.data).require.value
    case HC_UPDATE_ADD_HTLC_TAG => LightningMessageCodecs.updateAddHtlcCodec.decode(msg.data).require.value
    case HC_UPDATE_FULFILL_HTLC_TAG => LightningMessageCodecs.updateFulfillHtlcCodec.decode(msg.data).require.value
    case HC_UPDATE_FAIL_HTLC_TAG => LightningMessageCodecs.updateFailHtlcCodec.decode(msg.data).require.value
    case HC_UPDATE_FAIL_MALFORMED_HTLC_TAG => LightningMessageCodecs.updateFailMalformedHtlcCodec.decode(msg.data).require.value
    case HC_ERROR_TAG => LightningMessageCodecs.errorCodec.decode(msg.data).require.value
    case SWAP_IN_REQUEST_MESSAGE_TAG => provide(SwapInRequest).decode(msg.data).require.value
    case SWAP_IN_RESPONSE_MESSAGE_TAG => swapInResponseCodec.decode(msg.data).require.value
    case SWAP_IN_PAYMENT_REQUEST_MESSAGE_TAG => swapInPaymentRequestCodec.decode(msg.data).require.value
    case SWAP_IN_PAYMENT_DENIED_MESSAGE_TAG => swapInPaymentDeniedCodec.decode(msg.data).require.value
    case SWAP_IN_STATE_MESSAGE_TAG => swapInStateCodec.decode(msg.data).require.value
    case SWAP_OUT_REQUEST_MESSAGE_TAG => provide(SwapOutRequest).decode(msg.data).require.value
    case SWAP_OUT_FEERATES_MESSAGE_TAG => swapOutFeeratesCodec.decode(msg.data).require.value
    case SWAP_OUT_TRANSACTION_REQUEST_MESSAGE_TAG => swapOutTransactionRequestCodec.decode(msg.data).require.value
    case SWAP_OUT_TRANSACTION_RESPONSE_MESSAGE_TAG => swapOutTransactionResponseCodec.decode(msg.data).require.value
    case SWAP_OUT_TRANSACTION_DENIED_MESSAGE_TAG => swapOutTransactionDeniedCodec.decode(msg.data).require.value
    case otherwise => throw new RuntimeException(s"Can not decode tag=$otherwise")
  }

  def preEncode(msg: LightningMessage): LightningMessage = msg match {
    case msg: InvokeHostedChannel => UnknownMessage(HC_INVOKE_HOSTED_CHANNEL_TAG, invokeHostedChannelCodec.encode(msg).require)
    case msg: InitHostedChannel => UnknownMessage(HC_INIT_HOSTED_CHANNEL_TAG, initHostedChannelCodec.encode(msg).require)
    case msg: LastCrossSignedState => UnknownMessage(HC_LAST_CROSS_SIGNED_STATE_TAG, lastCrossSignedStateCodec.encode(msg).require)
    case msg: StateUpdate => UnknownMessage(HC_STATE_UPDATE_TAG, stateUpdateCodec.encode(msg).require)
    case msg: StateOverride => UnknownMessage(HC_STATE_OVERRIDE_TAG, stateOverrideCodec.encode(msg).require)
    case msg: HostedChannelBranding => UnknownMessage(HC_HOSTED_CHANNEL_BRANDING_TAG, hostedChannelBrandingCodec.encode(msg).require)
    case msg: RefundPending => UnknownMessage(HC_REFUND_PENDING_TAG, refundPendingCodec.encode(msg).require)
    case msg: ResizeChannel => UnknownMessage(HC_RESIZE_CHANNEL_TAG, resizeChannelCodec.encode(msg).require)
    case msg: QueryPublicHostedChannels => UnknownMessage(HC_QUERY_PUBLIC_HOSTED_CHANNELS_TAG, queryPublicHostedChannelsCodec.encode(msg).require)
    case msg: ReplyPublicHostedChannelsEnd => UnknownMessage(HC_REPLY_PUBLIC_HOSTED_CHANNELS_END_TAG, replyPublicHostedChannelsEndCodec.encode(msg).require)
    case msg: ChannelUpdate => UnknownMessage(PHC_UPDATE_SYNC_TAG, LightningMessageCodecs.channelUpdateCodec.encode(msg).require)
    case msg: UpdateAddHtlc => UnknownMessage(HC_UPDATE_ADD_HTLC_TAG, LightningMessageCodecs.updateAddHtlcCodec.encode(msg).require)
    case msg: UpdateFulfillHtlc => UnknownMessage(HC_UPDATE_FULFILL_HTLC_TAG, LightningMessageCodecs.updateFulfillHtlcCodec.encode(msg).require)
    case msg: UpdateFailHtlc => UnknownMessage(HC_UPDATE_FAIL_HTLC_TAG, LightningMessageCodecs.updateFailHtlcCodec.encode(msg).require)
    case msg: UpdateFailMalformedHtlc => UnknownMessage(HC_UPDATE_FAIL_MALFORMED_HTLC_TAG, LightningMessageCodecs.updateFailMalformedHtlcCodec.encode(msg).require)
    case msg: Error => UnknownMessage(HC_ERROR_TAG, LightningMessageCodecs.errorCodec.encode(msg).require)
    case SwapInRequest => UnknownMessage(SWAP_IN_REQUEST_MESSAGE_TAG, provide(SwapInRequest).encode(SwapInRequest).require)
    case msg: SwapInResponse => UnknownMessage(SWAP_IN_RESPONSE_MESSAGE_TAG, swapInResponseCodec.encode(msg).require)
    case msg: SwapInPaymentRequest => UnknownMessage(SWAP_IN_PAYMENT_REQUEST_MESSAGE_TAG, swapInPaymentRequestCodec.encode(msg).require)
    case msg: SwapInPaymentDenied => UnknownMessage(SWAP_IN_PAYMENT_DENIED_MESSAGE_TAG, swapInPaymentDeniedCodec.encode(msg).require)
    case msg: SwapInState => UnknownMessage(SWAP_IN_STATE_MESSAGE_TAG, swapInStateCodec.encode(msg).require)
    case SwapOutRequest => UnknownMessage(SWAP_OUT_REQUEST_MESSAGE_TAG, provide(SwapOutRequest).encode(SwapOutRequest).require)
    case msg: SwapOutFeerates => UnknownMessage(SWAP_OUT_FEERATES_MESSAGE_TAG, swapOutFeeratesCodec.encode(msg).require)
    case msg: SwapOutTransactionRequest => UnknownMessage(SWAP_OUT_TRANSACTION_REQUEST_MESSAGE_TAG, swapOutTransactionRequestCodec.encode(msg).require)
    case msg: SwapOutTransactionResponse => UnknownMessage(SWAP_OUT_TRANSACTION_RESPONSE_MESSAGE_TAG, swapOutTransactionResponseCodec.encode(msg).require)
    case msg: SwapOutTransactionDenied => UnknownMessage(SWAP_OUT_TRANSACTION_DENIED_MESSAGE_TAG, swapOutTransactionDeniedCodec.encode(msg).require)
    case _ => msg
  }
}
