package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    SecurityService securityService;
    @Mock
    ImageService imageService;
    @Mock
    SecurityRepository securityRepository;
    @Mock
    Sensor sensor;

    @Mock
    BufferedImage img;

    @Mock
    Set<Sensor> setOfSensors;

    @Spy
    Set<StatusListener> statusListeners = new HashSet<>();

    @Mock
    StatusListener statusListener;



    @BeforeEach
    public void init(){
        securityService = new SecurityService(securityRepository, imageService);
        setOfSensors = new HashSet<Sensor>();
    }

    //1.If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    public void alarmArmed_SensorActive_ThenStatusPending(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.handleSensorActivated();
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * 2.If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to alarm on. [This is the case where all sensors are deactivated and
     * then one gets activated]
     */
    @Test
    public void alarmArmed_NoSensorActiveToOneActive_ThenStatusPendingToActive(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.handleSensorActivated();
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //3.If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void pendingAlarm_AllSensorsDeactivate_ThenStatusPendingToNoAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.handleSensorDeactivated();
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);    }


   //4.If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void activeAlarm_SensorActiveDeactive_ThenNoChangeAlarmStatus(boolean sensorStatus){
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, sensorStatus);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 5.If a sensor is activated while already active and the system is in pending state,
     * change it to alarm state.
     * [This is the case where one sensor is already active and then another gets activated]
     */
    @Test
    public void alarmArmed_AlreadySensorActiveToAnotherActive_ThenStatusPendingToActive(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 6.If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void sensorDeactive_WhenAlreadyDeactivated_ThenNoStatusChange(){
        when(sensor.getActive()).thenReturn(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    /**
     * 7.If the camera image contains a cat while the system is armed-home,
     * put the system into alarm status.
     */
    @Test
    public void checkImageforCat_IfFound_ThenAlarmActive(){
        when(imageService.imageContainsCat(img, 50.0f)).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(img);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 8.If the camera image does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void checkImageForCat_IfNotFound_IfSensorInactive_ThenNoAlarm(){
        when(imageService.imageContainsCat(img, 50.0f)).thenReturn(false);
        when(securityService.isAnySensorActive()).thenReturn(false);
        securityService.processImage(img);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 9.If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void disarmed_ThenNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 10.If the system is armed, reset all sensors to inactive.
     */
    @Test
    public void armed_ThenAllSensorsInactive(){
     setOfSensors.add(sensor);
     when(securityRepository.getSensors()).thenReturn(setOfSensors);
     securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
     verify(sensor).setActive(false);
    }

    /**
     * 11.If the system is armed-home while the camera shows a cat,
     * set the alarm status to alarm.
     */
    @Test
    public void armed_catDetectedTrue_ThenAlarmActive(){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        lenient().when(imageService.imageContainsCat(img, 50.0f)).thenReturn(true);
        securityService.catDetected(true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Add Sensor
     */
    @Test
    public void addSensor_ToSetOfSensors(){
        securityService.addSensor(sensor);
        verify(securityRepository).addSensor(sensor);
    }

    /**
     * Remove Sensor
     */
    @Test
    public void removeSensor_FromSetOfSensors(){
        securityService.removeSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }

    /**
     * add statusListener
     */
    @Test
     public void addStatusListener_ToListOfStatusListener(){
      statusListeners.add(statusListener);
        Assertions.assertEquals(1, statusListeners.size());
        Mockito.verify(statusListeners).add(statusListener);
     }

    /**
     * remove statusListener
     */
    @Test
    public void removeStatusListener_FromListOfStatusListener(){
        statusListeners.add(statusListener);
        Assertions.assertEquals(1, statusListeners.size());
        statusListeners.remove(statusListener);
        Assertions.assertEquals(0, statusListeners.size());
        Mockito.verify(statusListeners).remove(statusListener);
    }

}
