<!-- Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. -->

<template>
  <div class="rk-profile-task">
    <div class="rk-profile-task-header">
      <div class="">服务</div>
      <div class="">端点名称</div>
      <div class="">监控时间</div>
      <div class="">监控持续时间<br />(min：1-15的整数）</div>
      <div class="">起始监控时间<br /> (ms)</div>
      <div class="">监控间隔<br />（ms：10及以上整数）</div>
      <div class="">最大采样数<br />（1-10整数）</div>
      <div>Delete</div>
    </div>
    <div v-if="list.length>0" class="rk-profile-task-conIf">
      <div class="rk-profile-task-content" v-for="(itemTo,i) in list" :key="i" >
        <div>{{itemTo.service}}</div>
        <div>{{itemTo.endpointName}}</div>
        <div>{{itemTo.monitorTime}}</div>
        <div>{{itemTo.monitorDuration}}</div>
        <div>{{itemTo.minThreshold}}</div>
        <div>{{itemTo.dumpPeriod}}</div>
        <div>{{itemTo.maxSamplingCount}}</div>
        <div class="rk-profile-task-delete" @click="taskDelete(i)">
          <span>删除</span>
        </div>
      </div>
    </div>
    <div v-else class="defaultText">{{this.$t('noData')}}</div>
    <!-- <div class="rk-profile-task-add" @click="taskAdd">
      +
    </div> -->
    <div class="rk-profile-task-btn">
      <div class="rk-profile-task-lead" @click="taskAdd">{{this.$t('taskBatchImport')}}
        <input type="file" @change="importExcel(this)"  accept=".csv, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel" id="intclass">
      </div>
      <div class="rk-profile-task-sub" @click="taskSub">
        {{this.$t('submit')}}
      </div>
    </div>
  </div>
</template>

<script lang="js">
import XLSX from 'xlsx'
import Axios, { AxiosResponse } from 'axios';
import * as profile from '@/graph/query/profile';
import * as option from '@/graph/query/option';
export default {
    name: 'ProfileTaskBacthAdd',
    props: { 
    },
    data() {
      return {
        list: [],
        services: [],
        listTo: [],
        durationTime: this.$store.getters['durationTime'],
      };
    },
    watch: {

    },
    created() {
      Axios.post('/graphql', {
        query: option['queryServices'],
        variables: {
          duration: this.durationTime,
          keyword: '',
        },
      }).then((res) => {
        if (!res.data.data) {
          return;
        }
        this.services = res.data.data.services
      })
    },
    computed: {

    },
    methods: {
      formateDate(param1, param2) {
        if (param1 && param1 !== '') {
          let date = new Date((param1 - 1) * 24 * 3600000)
          date.setHours(date.getHours() - 8)
          date.setYear(date.getFullYear() - 70)
          let o = {
              'M+': date.getMonth() + 1, // 月份
              'd+': date.getDate() - 1, // 日
              'h+': date.getHours(), // 小时
              'm+': date.getMinutes(), // 分
              's+': date.getSeconds(), // 秒
          };
          if (/(y+)/.test(param2)) {
              param2 = param2.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length));
          }
          for (let k in o) {
              if (new RegExp('(' + k + ')').test(param2)) {
                  param2 = param2.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)));
              }
          }
          return param2;
        }
      },
      taskAdd() {

      },
      taskDelete(item) {
        this.list.splice(item, 1)
        this.listTo.splice(item, 1)
      },
      taskSub() {
        let that = this
        if (this.listTo && this.listTo.length > 0) {
          this.listTo.forEach((item, index) => {
            const {
              service,  // 服务
              endpointName,   // 端点名称
              monitorTime,  // 监控时间
              monitorDuration, // 监控持续时间
              minThreshold,   // 起始监控时间 (ms)
              dumpPeriod,  // 监控间隔
              maxSamplingCount, // 最大采样数
            } = item;
            const creationRequest = {
              serviceId: service,
              endpointName,
              startTime: monitorTime,
              duration: monitorDuration,
              minDurationThreshold: Number(minThreshold),
              dumpPeriod: dumpPeriod,
              maxSamplingCount: maxSamplingCount,
            };
            Axios.post('/graphql', {
              query: profile['saveProfileTask'],
              variables: {
                creationRequest: creationRequest,
                duration: this.durationTime,
              },
            }).then((res) => {
              if (res.data.data && res.data.data.createTask && res.data.data.createTask.errorReason) {
                confirm(res.data.data.createTask.errorReason)
                return res.data.data.createTask
              }
              that.list.splice(index, 1)
              that.listTo.splice(index, 1)
              if (that.listTo.length - index === 1) {
                that.list.splice(0, 1)
                that.listTo.splice(0, 1)
                confirm('提交成功')
                that.$emit('closeSideboxBacthAdd');
              }
            });
          });
        } else {
          confirm('暂无可提交数据！')
        }
      },
      importExcel(obj) {
        let that = this;
        // let inputDOM = this.$refs.inputer;
        // 通过DOM取文件数据
        this.file = event.currentTarget.files[0];
        let rABS = false; // 是否将文件读取为二进制字符串
        let fileTo = this.file;
        this.fileName = fileTo.name
        let FReader = new FileReader();
        FileReader.prototype.readAsBinaryString = function(f) {
          let binary = '';
          let rABSTo = false; // 是否将文件读取为二进制字符串
          let pt = this;
          let wb; // 读取完成的数据
          let outdata;
          let FReaderTo = new FileReader();
          FReaderTo.onload = () => {
            let bytes = new Uint8Array(FReaderTo.result);
            let length = bytes.byteLength;
            for (let i = 0; i < length; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            if (rABSTo) {
                wb = XLSX.read(btoa(fixdata(binary)), { // 手动转化
                  type: 'base64',
                });
            } else {
                wb = XLSX.read(binary, {
                  type: 'binary',
                });
            }

            // console.log(XLSX.utils)
            outdata = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]); // outdata就是你想要的东西
            if (outdata && outdata.length > 0) {
              for (const i of Object.keys(outdata)) {
                if (that.services && that.services.length > 0) {
                  that.services.forEach(item => {
                    if (item.label.indexOf(outdata[i]['服务'].trim()) === 0) {
                      that.listTo.push({
                        service: item.key,  // 服务
                        endpointName: outdata[i]['端点名称'],   // 端点名称
                        monitorTime: new Date(that.formateDate(outdata[i]['监控时间'], 'yyyy-MM-dd hh:mm:ss')).getTime(),  // 监控时间
                        monitorDuration: outdata[i]['监控持续时间'], // 监控持续时间
                        minThreshold: outdata[i]['起始监控时间 (ms)'],   // 起始监控时间 (ms)
                        dumpPeriod: outdata[i]['监控间隔'],  // 监控间隔
                        maxSamplingCount: outdata[i]['最大采样数'], // 最大采样数
                      })
                      that.list.push({
                        service: outdata[i]['服务'],  // 服务
                        endpointName: outdata[i]['端点名称'],   // 端点名称
                        monitorTime: that.formateDate(outdata[i]['监控时间'], 'yyyy-MM-dd hh:mm:ss'),  // 监控时间
                        monitorDuration: outdata[i]['监控持续时间'], // 监控持续时间
                        minThreshold: outdata[i]['起始监控时间 (ms)'],   // 起始监控时间 (ms)
                        dumpPeriod: outdata[i]['监控间隔'],  // 监控间隔
                        maxSamplingCount: outdata[i]['最大采样数'], // 最大采样数
                      })
                    }
                  })
                }
              }
            }
          }
          event.target.value = ''
          FReaderTo.readAsArrayBuffer(f);
        }
        if (rABS) {
          FReader.readAsArrayBuffer(fileTo);
        } else {
          FReader.readAsBinaryString(fileTo);
        }
      },
    },
    mounted() {
      
    },
  };

  
</script>

<style lang="scss" scoped>
  .rk-profile-task {
    margin: 20px;
  }
  .rk-profile-task-header{
    display: flex;
    div{
      flex: 1;
      border: 1px solid #000;
      padding: 10px 0;
      text-align: center;
      border-right: none;
      align-items: center;
      justify-content: space-around;
      flex-direction: column;
      display: flex;
    }
    div:last-child{
      border-right: 1px solid #000;
    }
  }
  .rk-profile-task-conIf{
    padding-bottom: 130px;
  }
  .rk-profile-task-content{
    display: flex;
    div{
      flex: 1;
      text-align: center;
      border: 1px solid #ccc;
      border-top: none;
      line-height: 30px;
      border-right: none; 
      word-break: normal;
      word-wrap: break-word;
      overflow: hidden;
      align-items: center;
      justify-content: space-around;
      flex-direction: column;
      display: flex;
    }
    .rk-bar-select{
      margin: 0;
    }
    .rk-profile-task-time{
      // border-radius: 3px;
      border: 1px solid #ccc;
      width: 100%;
      .datepicker-popup{
        padding: 0 7%;
      }
    }
    .rk-profile-task-delete{
      // border-radius: 3px;
      border: 1px solid #ccc;
      text-align: center;
      vertical-align: middle;
      line-height: 28px;
      color: #448dfe;
      border-top: none;
      cursor: pointer;
    }
  }
  .rk-profile-task-add{
    border: 1px solid #000;
    text-align: center;
    line-height: 20px;
    font-size: 20px;
    cursor: pointer;
  }
  .rk-profile-task-btn{
    display: flex;
    justify-content: center;
    padding: 50px;
    position: fixed;
    bottom: 0;
    background: #ffffff;
    width: 100%;

    .rk-profile-task-lead{
      // margin: 0 0.3rem;
      line-height: 40px;
      height: 40px;
      border-radius: 3px;
      color: #ffffff;
      font-size: 20px;
      text-align: center;
      width: 300px;
      background-color: #0F89FF;
      position: relative;
      cursor: pointer;
      // margin: 100px auto;
      input{
        position: absolute;
        right: 0;
        top: 0;
        opacity: 0;
        width: 100%;
        height: 100%;
        cursor: pointer;
      }
    }
    .rk-profile-task-sub{
      line-height: 40px;
      height: 40px;
      border-radius: 3px;
      color: #ffffff;
      font-size: 20px;
      text-align: center;
      width: 300px;
      background-color: #0F89FF;
      margin-left: 20px;
      cursor: pointer; 
    }
  }
  .defaultText{
    text-align: center;
    line-height: 50px;
  }
</style>
