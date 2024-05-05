/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

otp.namespace("otp.layers");

otp.layers.OsmIdsStreetEdgeLayer = otp.Class({
  module: null,

  minimumZoomForStops: 15,

  url:
    otp.config.hostname +
    "/" +
    otp.config.restService +
    "/vectorTiles/osmids/{z}/{x}/{y}.pbf",

  initialize: function (module) {
    this.module = module;

    this.stopsLookup = {};

    this.layer = VectorTileLayer(this.url, {
      style: { stroke: true, color: "blue" },
    });

    this.module.webapp.map.layer_control.addOverlay(this.layer, "OSM Street IDs");

    this.layer.bindPopup("");

    this.layer.on("click", (e) => {
      e.originalEvent.preventDefault();
      this.layer.setPopupContent(
        this.getPopupContent({
          ...e.latlng,
          ...e.layer.feature.properties,
        })
      );
    });
  },

  getPopupContent: function (stop) {
    var this_ = this;

    var context = _.clone(stop);
    var popupContent = ich["otp-osmidsLayer-info"](context);

    return popupContent.get(0);
  },
});
