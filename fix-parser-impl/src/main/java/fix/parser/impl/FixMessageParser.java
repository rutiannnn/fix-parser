package fix.parser.impl;

import fix.parser.message.base.FixMessage;
import fix.parser.message.base.Segment;
import fix.parser.message.base.UnderlyingMessage;
import fix.parser.messages44.*;
import fix.parser.spec.FieldDef;
import fix.parser.spec.FixSpec;
import fix.parser.spec.FixType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static fix.parser.messages44.MessageTypes.*;

public class FixMessageParser {
    private static final byte FIELD_SEPARATOR = 0x01;  // SOH character
    private static final byte EQUALS_SIGN = 0x3D;

    private final FixSpec spec;

    public FixMessageParser(FixSpec spec) {
        this.spec = spec;
    }

    public FixMessage parse(byte[] messageBytes) {
        int fieldCount = countMaximumFields(messageBytes);

        int[] tags = new int[fieldCount];
        int[] valuePositions = new int[fieldCount];
        int[] valueLengths = new int[fieldCount];

        parseFields(messageBytes, tags, valuePositions, valueLengths);

        UnderlyingMessage underlyingMessage = new UnderlyingMessage(messageBytes, tags, valuePositions, valueLengths);

        Segment segment = new Segment(underlyingMessage, 0, fieldCount,
            parseRepeatingGroups(underlyingMessage, 0, fieldCount, tags, valuePositions, valueLengths)
        );

        int msgTypeIndex = findTagIndex(tags, Fields.MSGTYPE, 0, fieldCount);
        String msgType = new String(
            messageBytes,
            valuePositions[msgTypeIndex],
            valueLengths[msgTypeIndex],
            StandardCharsets.ISO_8859_1
        );

        return createMessage(msgType, segment);
    }

    private int countMaximumFields(byte[] messageBytes) {
        int count = 0;
        for (byte b : messageBytes) {
            if (b == FIELD_SEPARATOR) {
                count++;
            }
        }
        return count;
    }

    private void parseFields(byte[] messageBytes, int[] tags, int[] valuePositions, int[] valueLengths) {
        int fieldIndex = 0;
        int start = 0;

        while (start < messageBytes.length) {
            int equalsIndex = find(messageBytes, EQUALS_SIGN, start);

            tags[fieldIndex] = Integer.parseInt(
                new String(messageBytes, start, equalsIndex - start, StandardCharsets.ISO_8859_1)
            );
            valuePositions[fieldIndex] = equalsIndex + 1;

            final int separatorIndex;
            if (spec.fieldsByNumber().get(tags[fieldIndex]).type() == FixType.DATA) {
                valueLengths[fieldIndex] = Integer.parseInt(
                    new String(messageBytes, valuePositions[fieldIndex - 1], valueLengths[fieldIndex - 1], StandardCharsets.ISO_8859_1)
                );
                separatorIndex = valuePositions[fieldIndex] + valueLengths[fieldIndex];
            } else {
                separatorIndex = find(messageBytes, FIELD_SEPARATOR, start);
                valueLengths[fieldIndex] = separatorIndex - equalsIndex - 1;
            }

            fieldIndex++;
            start = separatorIndex + 1;
        }
    }

    private int findTagIndex(int[] tags, int targetTag, int start, int end) {
        for (int i = start; i < end; i++) {
            if (tags[i] == targetTag) {
                return i;
            }
        }
        return -1;
    }

    private int find(byte[] bytes, byte target, int start) {
        final int len = bytes.length;
        int i = start;

        // Process 8 bytes at a time
        for (; i <= len - 8; i += 8) {
            if (bytes[i] == target) return i;
            if (bytes[i + 1] == target) return i + 1;
            if (bytes[i + 2] == target) return i + 2;
            if (bytes[i + 3] == target) return i + 3;
            if (bytes[i + 4] == target) return i + 4;
            if (bytes[i + 5] == target) return i + 5;
            if (bytes[i + 6] == target) return i + 6;
            if (bytes[i + 7] == target) return i + 7;
        }

        // Handle remaining bytes
        for (; i < len; i++) {
            if (bytes[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private FixMessage createMessage(String msgType, Segment segment) {
        return switch (msgType) {
            case HEARTBEAT -> new HeartbeatMessage(segment);
            case LOGON -> new LogonMessage(segment);
            case TESTREQUEST -> new TestRequestMessage(segment);
            case RESENDREQUEST -> new ResendRequestMessage(segment);
            case REJECT -> new RejectMessage(segment);
            case SEQUENCERESET -> new SequenceResetMessage(segment);
            case LOGOUT -> new LogoutMessage(segment);
            case BUSINESSMESSAGEREJECT -> new BusinessMessageRejectMessage(segment);
            case USERREQUEST -> new UserRequestMessage(segment);
            case USERRESPONSE -> new UserResponseMessage(segment);
            case ADVERTISEMENT -> new AdvertisementMessage(segment);
            case INDICATIONOFINTEREST -> new IndicationOfInterestMessage(segment);
            case NEWS -> new NewsMessage(segment);
            case EMAIL -> new EmailMessage(segment);
            case QUOTEREQUEST -> new QuoteRequestMessage(segment);
            case QUOTERESPONSE -> new QuoteResponseMessage(segment);
            case QUOTEREQUESTREJECT -> new QuoteRequestRejectMessage(segment);
            case RFQREQUEST -> new RFQRequestMessage(segment);
            case QUOTE -> new QuoteMessage(segment);
            case QUOTECANCEL -> new QuoteCancelMessage(segment);
            case QUOTESTATUSREQUEST -> new QuoteStatusRequestMessage(segment);
            case QUOTESTATUSREPORT -> new QuoteStatusReportMessage(segment);
            case MASSQUOTE -> new MassQuoteMessage(segment);
            case MASSQUOTEACKNOWLEDGEMENT -> new MassQuoteAcknowledgementMessage(segment);
            case MARKETDATAREQUEST -> new MarketDataRequestMessage(segment);
            case MARKETDATASNAPSHOTFULLREFRESH -> new MarketDataSnapshotFullRefreshMessage(segment);
            case MARKETDATAINCREMENTALREFRESH -> new MarketDataIncrementalRefreshMessage(segment);
            case MARKETDATAREQUESTREJECT -> new MarketDataRequestRejectMessage(segment);
            case SECURITYDEFINITIONREQUEST -> new SecurityDefinitionRequestMessage(segment);
            case SECURITYDEFINITION -> new SecurityDefinitionMessage(segment);
            case SECURITYTYPEREQUEST -> new SecurityTypeRequestMessage(segment);
            case SECURITYTYPES -> new SecurityTypesMessage(segment);
            case SECURITYLISTREQUEST -> new SecurityListRequestMessage(segment);
            case SECURITYLIST -> new SecurityListMessage(segment);
            case DERIVATIVESECURITYLISTREQUEST -> new DerivativeSecurityListRequestMessage(segment);
            case DERIVATIVESECURITYLIST -> new DerivativeSecurityListMessage(segment);
            case SECURITYSTATUSREQUEST -> new SecurityStatusRequestMessage(segment);
            case SECURITYSTATUS -> new SecurityStatusMessage(segment);
            case TRADINGSESSIONSTATUSREQUEST -> new TradingSessionStatusRequestMessage(segment);
            case TRADINGSESSIONSTATUS -> new TradingSessionStatusMessage(segment);
            case NEWORDERSINGLE -> new NewOrderSingleMessage(segment);
            case EXECUTIONREPORT -> new ExecutionReportMessage(segment);
            case DONTKNOWTRADE -> new DontKnowTradeMessage(segment);
            case ORDERCANCELREPLACEREQUEST -> new OrderCancelReplaceRequestMessage(segment);
            case ORDERCANCELREQUEST -> new OrderCancelRequestMessage(segment);
            case ORDERCANCELREJECT -> new OrderCancelRejectMessage(segment);
            case ORDERSTATUSREQUEST -> new OrderStatusRequestMessage(segment);
            case ORDERMASSCANCELREQUEST -> new OrderMassCancelRequestMessage(segment);
            case ORDERMASSCANCELREPORT -> new OrderMassCancelReportMessage(segment);
            case ORDERMASSSTATUSREQUEST -> new OrderMassStatusRequestMessage(segment);
            case NEWORDERCROSS -> new NewOrderCrossMessage(segment);
            case CROSSORDERCANCELREPLACEREQUEST -> new CrossOrderCancelReplaceRequestMessage(segment);
            case CROSSORDERCANCELREQUEST -> new CrossOrderCancelRequestMessage(segment);
            case NEWORDERMULTILEG -> new NewOrderMultilegMessage(segment);
            case MULTILEGORDERCANCELREPLACEREQUEST -> new MultilegOrderCancelReplaceRequestMessage(segment);
            case BIDREQUEST -> new BidRequestMessage(segment);
            case BIDRESPONSE -> new BidResponseMessage(segment);
            case NEWORDERLIST -> new NewOrderListMessage(segment);
            case LISTSTRIKEPRICE -> new ListStrikePriceMessage(segment);
            case LISTSTATUS -> new ListStatusMessage(segment);
            case LISTEXECUTE -> new ListExecuteMessage(segment);
            case LISTCANCELREQUEST -> new ListCancelRequestMessage(segment);
            case LISTSTATUSREQUEST -> new ListStatusRequestMessage(segment);
            case ALLOCATIONINSTRUCTION -> new AllocationInstructionMessage(segment);
            case ALLOCATIONINSTRUCTIONACK -> new AllocationInstructionAckMessage(segment);
            case ALLOCATIONREPORT -> new AllocationReportMessage(segment);
            case ALLOCATIONREPORTACK -> new AllocationReportAckMessage(segment);
            case CONFIRMATION -> new ConfirmationMessage(segment);
            case CONFIRMATIONACK -> new ConfirmationAckMessage(segment);
            case CONFIRMATIONREQUEST -> new ConfirmationRequestMessage(segment);
            case SETTLEMENTINSTRUCTIONS -> new SettlementInstructionsMessage(segment);
            case SETTLEMENTINSTRUCTIONREQUEST -> new SettlementInstructionRequestMessage(segment);
            case TRADECAPTUREREPORTREQUEST -> new TradeCaptureReportRequestMessage(segment);
            case TRADECAPTUREREPORTREQUESTACK -> new TradeCaptureReportRequestAckMessage(segment);
            case TRADECAPTUREREPORT -> new TradeCaptureReportMessage(segment);
            case TRADECAPTUREREPORTACK -> new TradeCaptureReportAckMessage(segment);
            case REGISTRATIONINSTRUCTIONS -> new RegistrationInstructionsMessage(segment);
            case REGISTRATIONINSTRUCTIONSRESPONSE -> new RegistrationInstructionsResponseMessage(segment);
            case POSITIONMAINTENANCEREQUEST -> new PositionMaintenanceRequestMessage(segment);
            case POSITIONMAINTENANCEREPORT -> new PositionMaintenanceReportMessage(segment);
            case REQUESTFORPOSITIONS -> new RequestForPositionsMessage(segment);
            case REQUESTFORPOSITIONSACK -> new RequestForPositionsAckMessage(segment);
            case POSITIONREPORT -> new PositionReportMessage(segment);
            case ASSIGNMENTREPORT -> new AssignmentReportMessage(segment);
            case COLLATERALREQUEST -> new CollateralRequestMessage(segment);
            case COLLATERALASSIGNMENT -> new CollateralAssignmentMessage(segment);
            case COLLATERALRESPONSE -> new CollateralResponseMessage(segment);
            case COLLATERALREPORT -> new CollateralReportMessage(segment);
            case COLLATERALINQUIRY -> new CollateralInquiryMessage(segment);
            case NETWORKSTATUSREQUEST -> new NetworkStatusRequestMessage(segment);
            case NETWORKSTATUSRESPONSE -> new NetworkStatusResponseMessage(segment);
            case COLLATERALINQUIRYACK -> new CollateralInquiryAckMessage(segment);
            default -> throw new IllegalArgumentException("Unsupported message type: " + msgType);
        };
    }

    private Segment[] parseRepeatingGroups(UnderlyingMessage message, int start, int end,
                                           int[] tags, int[] valuePositions, int[] valueLengths) {
        final List<Segment> groups = new ArrayList<>();

        for (int i = start; i < end; i++) {
            // Check if current tag is a repeating group counter (NoXXX field)
            FieldDef field = spec.fieldsByNumber().get(tags[i]);
            if (field != null && field.type() == FixType.NUMINGROUP) {
                int numInGroup = Integer.parseInt(
                    new String(message.rawMessage(), valuePositions[i], valueLengths[i], StandardCharsets.ISO_8859_1)
                );

                if (numInGroup > 0) {
                    // First field after counter is the first field of the group
                    int firstGroupTag = tags[i + 1];
                    int currentPos = i + 1;

                    // Process each instance of the group
                    for (int groupIndex = 0; groupIndex < numInGroup; groupIndex++) {
                        int groupStart = currentPos;

                        // Find the end of this group instance
                        int groupEnd = findGroupEnd(tags, firstGroupTag, groupStart, end);

                        // Recursively parse nested groups within this group instance
                        Segment[] nestedGroups = parseRepeatingGroups(message, groupStart, groupEnd, tags, valuePositions, valueLengths);

                        // Create segment for this group instance with its nested groups
                        groups.add(new Segment(message, groupStart, groupEnd, nestedGroups));

                        currentPos = groupEnd;
                    }

                    // Skip processed fields
                    i = currentPos - 1;
                }
            }
        }

        return groups.toArray(new Segment[0]);
    }

    private int findGroupEnd(int[] tags, int firstGroupTag, int start, int end) {
        int pos = start + 1;
        while (pos < end) {
            // If we find the first tag again, it's the start of the next group instance
            if (tags[pos] == firstGroupTag) {
                return pos;
            }
            pos++;
        }
        return end;
    }
}
