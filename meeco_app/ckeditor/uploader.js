class MeecoUploadAdapter {
  constructor(loader) {
    this.loader = loader;
    this.handlers = {};

    window.onMessageReceive = (handle, error, data) => {
      if (error) {
        this.handlers[handle].reject(new Error(error));
      } else {
        console.log("Data: " + data);
        this.handlers[handle].resolve({
          default: data
        });
      }
      delete this.handlers[handle];
    };
  }

  upload() {
    console.log(this.loader);
    return this.loader.file.then(file => {
      return this.waitForOS(this.loader.id, file);
    });
  }

  waitForOS(id, file) {
    return new Promise((resolve, reject) => {
      this.handlers[id] = { resolve, reject };
      var reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = function() {
        FlutterChannel.postImage(JSON.stringify({
          id,
          file: reader.result.split(",")[1]
        }));
      };
      reader.onerror = function(error) {
        console.log("Error: ", error);
      };
    });
  }
}
