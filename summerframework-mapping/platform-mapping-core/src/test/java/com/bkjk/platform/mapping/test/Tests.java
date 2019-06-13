package com.bkjk.platform.mapping.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.bkjk.platform.mapping.MappingAutoConfiguration;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestApplication.class, MappingAutoConfiguration.class})
@Slf4j
public class Tests {
    @Autowired
    MapperFacade mapper;

    @Test
    public void configureTest2() {
        Source source = new Source();
        Dest dest = new Dest();
        mapper.map(source, dest);
        assertThat(dest.getI(), equalTo(123L));
        assertThat(dest.getS(), equalTo("test"));
    }

    @Test
    public void dateTest1() {
        DateTest1 dateTest1 = new DateTest1();
        dateTest1.setDate("2019-01-02");
        dateTest1.setDateTime("2019-01-02 23:22:21.123");
        dateTest1.setTime("23:22:21");
        DateTest2 dateTest2 = mapper.map(dateTest1, DateTest2.class);

        assertThat(dateTest2.getLocalDate(), equalTo(LocalDate.of(2019, 1, 2)));
        assertThat(dateTest2.getLocalTime(), equalTo(LocalTime.of(23, 22, 21)));
        assertThat(dateTest2.getLocalDateTime(), equalTo(LocalDateTime.of(2019, 1, 2, 23, 22, 21, 123000000)));

    }

    @Test
    public void dateTest2() {
        DateTest2 dateTest2 = new DateTest2();
        dateTest2.setLocalDate(LocalDate.of(2019, 1, 2));
        dateTest2.setLocalTime(LocalTime.of(23, 22, 21));
        dateTest2.setLocalDateTime(LocalDateTime.of(2019, 1, 2, 23, 22, 21, 123000000));
        DateTest1 dateTest1 = mapper.map(dateTest2, DateTest1.class);

        assertThat(dateTest1.getDate(), equalTo("2019-01-02"));
        assertThat(dateTest1.getTime(), equalTo("23:22:21"));
        assertThat(dateTest1.getDateTime(), equalTo("2019-01-02 23:22:21.123"));
    }

    @Test
    public void enumTest1() {

        EnumTest1 enumTest1 = new EnumTest1();
        enumTest1.setIntEnum(IntEnum.ERR_TOO_OFTEN.getCode());
        enumTest1.setStringEnum(StringEnum.REFUND_SUCC.getInfo());
        enumTest1.setThreeFieldsEnum(ThreeFieldsEnum.NO_VIP_QUICKPAY.getKey());
        enumTest1.setSimpleEnum(SimpleEnum.BALANCE_PAY.name());
        EnumTest2 enumTest2 = mapper.map(enumTest1, EnumTest2.class);

        assertThat(enumTest2.getIntEnum(), equalTo(IntEnum.ERR_TOO_OFTEN));
        assertThat(enumTest2.getThreeFieldsEnum(), equalTo(ThreeFieldsEnum.NO_VIP_QUICKPAY));
        assertThat(enumTest2.getSimpleEnum(), equalTo(SimpleEnum.BALANCE_PAY));
        assertThat(enumTest2.getStringEnum(), equalTo(StringEnum.REFUND_SUCC));
    }

    @Test
    public void enumTest2() {
        EnumTest2 enumTest2 = new EnumTest2();
        enumTest2.setIntEnum(IntEnum.ERR_TOO_OFTEN);
        enumTest2.setStringEnum(StringEnum.REFUND_SUCC);
        enumTest2.setThreeFieldsEnum(ThreeFieldsEnum.NO_VIP_QUICKPAY);
        enumTest2.setSimpleEnum(SimpleEnum.BALANCE_PAY);
        EnumTest1 enumTest1 = mapper.map(enumTest2, EnumTest1.class);

        assertThat(enumTest1.getIntEnum(), equalTo(IntEnum.ERR_TOO_OFTEN.getCode()));
        assertThat(enumTest1.getThreeFieldsEnum(), equalTo(ThreeFieldsEnum.NO_VIP_QUICKPAY.getKey()));
        assertThat(enumTest1.getSimpleEnum(), equalTo(SimpleEnum.BALANCE_PAY.name()));
        assertThat(enumTest1.getStringEnum(), equalTo(StringEnum.REFUND_SUCC.getInfo()));
    }

    @Test
    public void skipTest1() {
        SkipTest1 skipTest1 = new SkipTest1();
        skipTest1.setAa("aa");
        skipTest1.setBb("bb");
        skipTest1.setTest("test");
        SkipTest2 skipTest2 = mapper.map(skipTest1, SkipTest2.class);
        assertThat(skipTest2.getAaa(), equalTo(null));
        assertThat(skipTest2.getBb(), equalTo(null));
        assertThat(skipTest2.getTestFoo(), equalTo("test"));
    }

    @Test
    public void skipTest2() {
        SkipTest2 skipTest2 = new SkipTest2();
        skipTest2.setAaa("aa");
        skipTest2.setBb("bb");
        skipTest2.setTestFoo("test");
        SkipTest1 skipTest1 = mapper.map(skipTest2, SkipTest1.class);
        assertThat(skipTest1.getAa(), equalTo("aa"));
        assertThat(skipTest1.getBb(), equalTo(null));
        assertThat(skipTest1.getTest(), equalTo("test"));
    }

    @Test
    public void streamTest() {
        List<Dest> destList =
            IntStream.rangeClosed(1, 10).mapToObj(i -> new Source("test" + i, i)).collect(Collectors.toList()).stream()
                .map(item -> mapper.map(item, Dest.class)).collect(Collectors.toList());
        assertThat(destList.get(0).getS(), equalTo("test1"));
    }
}
